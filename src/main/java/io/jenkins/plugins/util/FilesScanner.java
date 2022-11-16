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

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

/**
 * Finds all files that match a specified Ant files pattern and delegates these file to the actual processing method
 * {@link #processFile(Path, Charset, FilteredLog)}. This callable will be invoked on an agent so all fields and the
 * returned list of results need to be {@link Serializable}.
 *
 * @param <T>
 *         the type of the results
 *
 * @author Ullrich Hafner
 */
// FIXME: check if we should rather use NIO package than Ant
public abstract class FilesScanner<T extends Serializable>
        extends MasterToSlaveFileCallable<FilesScanner.ScannerResult<T>> {
    private static final long serialVersionUID = 2216842481400265078L;

    private final String filePattern;
    private final String encoding;
    private final boolean followSymbolicLinks;

    /**
     * Creates a new instance of {@link FilesScanner}.
     *
     * @param filePattern
     *         ant file-set pattern to scan for files to parse
     * @param encoding
     *         encoding of the files to parse
     * @param followSymbolicLinks
     *         if the scanner should traverse symbolic links
     */
    protected FilesScanner(final String filePattern, final String encoding, final boolean followSymbolicLinks) {
        super();

        this.filePattern = filePattern;
        this.encoding = encoding;
        this.followSymbolicLinks = followSymbolicLinks;
    }

    @Override
    public final ScannerResult<T> invoke(final File workspace, final VirtualChannel channel) {
        FilteredLog log = new FilteredLog("Errors during parsing");
        log.logInfo("Searching for all files in '%s' that match the pattern '%s'",
                workspace.getAbsolutePath(), filePattern);
        log.logInfo("Traversing of symbolic links: %s", followSymbolicLinks ? "enabled" : "disabled");

        String[] fileNames = new FileFinder(filePattern, StringUtils.EMPTY, followSymbolicLinks).find(workspace);
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
            Path file = workspace.toPath().resolve(fileName);

            if (!Files.isReadable(file)) {
                log.logError("Skipping file '%s' because Jenkins has no permission to read the file", fileName);
            }
            else if (isEmpty(file)) {
                log.logError("Skipping file '%s' because it's empty", fileName);
            }
            else {
                results.add(processFile(file, new CharsetValidation().getCharset(encoding), log));
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
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    protected String plural(final int count, final String itemName) {
        return String.format("%d %s%s", count, itemName, count == 1 ? "" : "s");
    }

    protected abstract T processFile(Path file, Charset charset, FilteredLog log);

    private boolean isEmpty(final Path file) {
        try {
            return Files.size(file) <= 0;
        }
        catch (IOException e) {
            return true;
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
            return results;
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

    /**
     * Scans the workspace and finds all files matching a given ant pattern.
     *
     * @author Ullrich Hafner
     */
    private static class FileFinder extends MasterToSlaveFileCallable<String[]> {
        private static final long serialVersionUID = 2970029366847565970L;

        private final String includesPattern;
        private final String excludesPattern;
        private final boolean followSymlinks;

        FileFinder(final String includesPattern) {
            this(includesPattern, StringUtils.EMPTY);
        }

        FileFinder(final String includesPattern, final String excludesPattern) {
            this(includesPattern, excludesPattern, false);
        }

        FileFinder(final String includesPattern, final String excludesPattern, final boolean followSymlinks) {
            super();

            this.includesPattern = includesPattern;
            this.excludesPattern = excludesPattern;
            this.followSymlinks = followSymlinks;
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
                fileSet.setFollowSymlinks(followSymlinks);

                return fileSet.getDirectoryScanner(antProject).getIncludedFiles();
            }
            catch (BuildException ignored) {
                return new String[0]; // as fallback do not return any file
            }
        }
    }
}
