package io.jenkins.plugins.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.TypeSelector;
import org.apache.tools.ant.types.selectors.TypeSelector.FileType;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import io.jenkins.plugins.util.AgentFileVisitor.ScannerResult;

/**
 * Finds all files that match a specified Ant file pattern and visits these files with the processing method
 * {@link #processFile(Path, Charset, FilteredLog)}, that has to be implemented by concrete subclasses. This callable
 * will be invoked on an agent so all fields and the returned list of results need to be {@link Serializable}.
 *
 * @param <T>
 *         the type of the results
 *
 * @author Ullrich Hafner
 */
public abstract class AgentFileVisitor<T extends Serializable>
        extends MasterToSlaveFileCallable<ScannerResult<T>> {
    private static final long serialVersionUID = 2216842481400265078L;

    private final String filePattern;
    private final String encoding;
    private final boolean followSymbolicLinks;
    private final boolean errorOnEmptyFiles;
    private final FileSystemFacade fileSystemFacade;
    private static final String EMPTY_FILE = "Skipping file '%s' because it's empty";

    /**
     * Creates a new instance of {@link AgentFileVisitor}.
     * @param filePattern
     *         ant file-set pattern to scan for files to parse
     * @param encoding
     *         encoding of the files to parse
     * @param followSymbolicLinks
     * @param errorOnEmptyFiles
     */
    protected AgentFileVisitor(final String filePattern, final String encoding, final boolean followSymbolicLinks, final boolean errorOnEmptyFiles) {
        this(filePattern, encoding, followSymbolicLinks, errorOnEmptyFiles, new FileSystemFacade());
    }

    @VisibleForTesting
    AgentFileVisitor(final String filePattern, final String encoding, final boolean followSymbolicLinks,
                     boolean errorOnEmptyFiles, final FileSystemFacade fileSystemFacade) {
        super();

        this.filePattern = filePattern;
        this.encoding = encoding;
        this.followSymbolicLinks = followSymbolicLinks;
        this.errorOnEmptyFiles = errorOnEmptyFiles;
        this.fileSystemFacade = fileSystemFacade;
    }

    @Override
    public final ScannerResult<T> invoke(final File workspace, final VirtualChannel channel) {
        FilteredLog log = new FilteredLog("Errors during parsing");
        log.logInfo("Searching for all files in '%s' that match the pattern '%s'",
                fileSystemFacade.getAbsolutePath(workspace), filePattern);
        log.logInfo("Traversing of symbolic links: %s", followSymbolicLinks ? "enabled" : "disabled");

        String[] fileNames = fileSystemFacade.find(filePattern, followSymbolicLinks, workspace);
        if (fileNames.length == 0) {
            log.logError("No files found for pattern '%s'. Configuration error?", filePattern);

            return new ScannerResult<>(log);
        }
        else {
            log.logInfo("-> found %s", plural(fileNames.length, "file"));

            return new ScannerResult<>(log, scanFiles(workspace, fileNames, log));
        }
    }

    private List<T> scanFiles(final File workspace, final String[] fileNames, final FilteredLog log) {
        List<T> results = new ArrayList<>();
        for (String fileName : fileNames) {
            Path file = fileSystemFacade.resolve(workspace, fileName);

            if (fileSystemFacade.isNotReadable(file)) {
                log.logError("Skipping file '%s' because Jenkins has no permission to read the file", fileName);
            }
            else if (fileSystemFacade.isEmpty(file)) {
                if (errorOnEmptyFiles) {
                    log.logError(EMPTY_FILE, fileName);
                }
                else {
                    log.logInfo(EMPTY_FILE, fileName);
                }
            }
            else {
                results.add(processFile(file, new CharsetValidation().getCharset(encoding), log));
                log.logInfo("Successfully processed file '%s'", fileName);
            }
        }
        return results;
    }

    /**
     * Creates the correct singular or plural form of the specified word depending on the size of the elements.
     *
     * @param count
     *         the count of elements
     * @param itemName
     *         the name of the items (singular)
     *
     * @return the message
     */
    protected String plural(final int count, @SuppressWarnings("SameParameterValue") final String itemName) {
        return String.format("%d %s%s", count, itemName, count == 1 ? "" : "s");
    }

    protected abstract T processFile(Path file, Charset charset, FilteredLog log);

    /**
     * File system facade that can be replaced by a stub in unit tests.
     */
    static class FileSystemFacade implements Serializable {
        private static final long serialVersionUID = 4052720703351280685L;

        String getAbsolutePath(final File file) {
            return file.getAbsolutePath();
        }

        String[] find(final String includesPattern, final boolean followSymbolicLinks, final File workspace) {
            return new FileFinder(includesPattern, StringUtils.EMPTY, followSymbolicLinks).find(workspace);
        }

        Path resolve(final File folder, final String fileName) {
            return folder.toPath().resolve(fileName);
        }

        boolean isNotReadable(final Path file) {
            return !Files.isReadable(file);
        }

        boolean isEmpty(final Path file) {
            try {
                return Files.size(file) <= 0;
            }
            catch (IOException e) {
                return true;
            }
        }
    }

    /**
     * Scans the workspace and finds all files matching a given ant pattern.
     *
     * @author Ullrich Hafner
     */
    static class FileFinder extends MasterToSlaveFileCallable<String[]> {
        private static final long serialVersionUID = 2970029366847565970L;

        private final String includesPattern;
        private final String excludesPattern;
        private final boolean followSymbolicLinks;

        FileFinder(final String includesPattern, final String excludesPattern) {
            this(includesPattern, excludesPattern, false);
        }

        FileFinder(final String includesPattern, final String excludesPattern, final boolean followSymbolicLinks) {
            super();

            this.includesPattern = includesPattern;
            this.excludesPattern = excludesPattern;
            this.followSymbolicLinks = followSymbolicLinks;
        }

        /**
         * Returns an array with the file names of the specified file pattern that have been found in the workspace.
         *
         * @param workspace
         *         root directory of the workspace
         * @param channel
         *         not used
         *
         * @return the file names of all found files
         */
        @Override
        public String[] invoke(final File workspace, final VirtualChannel channel) {
            return find(workspace);
        }

        /**
         * Returns an array with the file names of the specified file pattern that have been found in the workspace.
         *
         * @param workspace
         *         root directory of the workspace
         *
         * @return the file names of all found files
         */
        public String[] find(final File workspace) {
            try {
                FileSet fileSet = new FileSet();
                Project antProject = new Project();
                fileSet.setProject(antProject);
                fileSet.setDir(workspace);
                fileSet.setIncludes(includesPattern);
                TypeSelector selector = new TypeSelector();
                FileType fileType = new FileType();
                fileType.setValue(FileType.FILE);
                selector.setType(fileType);
                fileSet.addType(selector);
                if (StringUtils.isNotBlank(excludesPattern)) {
                    fileSet.setExcludes(excludesPattern);
                }
                fileSet.setFollowSymlinks(followSymbolicLinks);

                return fileSet.getDirectoryScanner(antProject).getIncludedFiles();
            }
            catch (BuildException ignored) {
                return new String[0]; // as fallback do not return any file
            }
        }
    }

    /**
     * The results for all found files. Logging messages that have been recorded during the scanning process will be
     * also available.
     *
     * @param <T>
     *         the type of the results
     */
    public static class ScannerResult<T extends Serializable> implements Serializable {
        private static final long serialVersionUID = 2122230867938547733L;

        private final FilteredLog log;
        private final List<T> results;

        ScannerResult(final FilteredLog log) {
            this(log, Collections.emptyList());
        }

        ScannerResult(final FilteredLog log, final List<T> results) {
            this.log = log;
            this.results = new ArrayList<>(results);
        }

        public FilteredLog getLog() {
            return log;
        }

        public List<T> getResults() {
            return Collections.unmodifiableList(results);
        }

        /**
         * Returns whether there have been error messages recorded.
         *
         * @return {@code true} if error messages have been recorded, {@code false} otherwise
         */
        public boolean hasErrors() {
            return !getLog().getErrorMessages().isEmpty();
        }
    }
}
