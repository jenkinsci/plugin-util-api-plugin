package io.jenkins.plugins.util;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SerializableTest;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.jenkins.plugins.util.AgentFileVisitor.FileSystemFacade;
import io.jenkins.plugins.util.AgentFileVisitor.FileVisitorResult;
import io.jenkins.plugins.util.AgentFileVisitorTest.StringScanner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link AgentFileVisitor}.
 *
 * @author Ullrich Hafner
 */
class AgentFileVisitorTest extends SerializableTest<StringScanner> {
    private static final String CONTENT = "Hello World!";
    private static final String PATTERN = "**/*.txt";
    private static final String ENCODING = "UTF-8";

    @TempDir
    private File workspace;

    @DisplayName("Should report error on empty results")
    @CsvSource({"true, enabled", "false, disabled"})
    @ParameterizedTest(name = "{index} => followSymbolicLinks={0}, message={1}")
    void shouldReportErrorOnEmptyResults(final boolean followLinks, final String message) {
        var scanner = new StringScanner(PATTERN, ENCODING, followLinks, true,
                createFileSystemFacade(followLinks));

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);

        assertThat(actualResult.getResults()).isEmpty();
        assertThat(actualResult.getLog().getInfoMessages()).containsExactly(
                "Searching for all files in '/absolute/path' that match the pattern '" + PATTERN + "'",
                "Traversing of symbolic links: " + message);
        assertThat(actualResult.getLog().getErrorMessages()).containsExactly(
                "Errors during parsing",
                "No files found for pattern '**/*.txt'. Configuration error?");
        assertThat(actualResult.hasErrors()).isTrue();
    }

    @DisplayName("Should report error on single result")
    @CsvSource({"true, enabled", "false, disabled"})
    @ParameterizedTest(name = "{index} => followSymbolicLinks={0}, message={1}")
    void shouldReturnSingleResult(final boolean followLinks, final String message) {
        var scanner = new StringScanner(PATTERN, ENCODING, followLinks, true,
                createFileSystemFacade(followLinks, "/one.txt"));

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);
        assertThat(actualResult.getResults()).containsExactly(CONTENT + 1);
        assertThat(actualResult.getLog().getInfoMessages()).containsExactly(
                "Searching for all files in '/absolute/path' that match the pattern '**/*.txt'",
                "Traversing of symbolic links: " + message,
                "-> found 1 file",
                "Successfully processed file '/one.txt'");
        assertThat(actualResult.getLog().getErrorMessages()).isEmpty();
        assertThat(actualResult.hasErrors()).isFalse();
    }

    @DisplayName("Should report error on single result")
    @CsvSource({"true, enabled", "false, disabled"})
    @ParameterizedTest(name = "{index} => followSymbolicLinks={0}, message={1}")
    void shouldReturnMultipleResults(final boolean followLinks, final String message) {
        var scanner = new StringScanner(PATTERN, ENCODING, followLinks, true,
                createFileSystemFacade(followLinks, "/one.txt", "/two.txt"));

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);
        assertThat(actualResult.getResults()).containsExactly(CONTENT + 1, CONTENT + 2);
        assertThat(actualResult.getLog().getInfoMessages()).containsExactly(
                "Searching for all files in '/absolute/path' that match the pattern '**/*.txt'",
                "Traversing of symbolic links: " + message,
                "-> found 2 files",
                "Successfully processed file '/one.txt'",
                "Successfully processed file '/two.txt'");
        assertThat(actualResult.getLog().getErrorMessages()).isEmpty();
        assertThat(actualResult.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Should log error for empty or forbidden files")
    void shouldLogErrorForEmptyAndForbiddenFiles() {
        var fileSystemFacade = createFileSystemFacade(true,
                "/one.txt", "/two.txt", "empty.txt", "not-readable.txt");

        var empty = workspace.toPath().resolve("empty.txt");
        when(fileSystemFacade.resolve(workspace, "empty.txt")).thenReturn(empty);
        when(fileSystemFacade.isEmpty(empty)).thenReturn(true);

        var notReadable = workspace.toPath().resolve("not-readable.txt");
        when(fileSystemFacade.resolve(workspace, "not-readable.txt")).thenReturn(notReadable);
        when(fileSystemFacade.isNotReadable(notReadable)).thenReturn(true);

        var scanner = new StringScanner(PATTERN, ENCODING, true, true,
                fileSystemFacade);

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);
        assertThat(actualResult.getResults()).containsExactly(CONTENT + 1, CONTENT + 2);
        assertThat(actualResult.getLog().getInfoMessages()).contains(
                "Searching for all files in '/absolute/path' that match the pattern '**/*.txt'",
                "-> found 4 files",
                "Successfully processed file '/one.txt'",
                "Successfully processed file '/two.txt'");
        assertThat(actualResult.hasErrors()).isTrue();
        assertThat(actualResult.getLog().getErrorMessages()).containsExactly("Errors during parsing",
                "Skipping file 'empty.txt' because it's empty",
                "Skipping file 'not-readable.txt' because Jenkins has no permission to read the file");
    }

    @Test
    @DisplayName("Should skip logging of errors when parsing empty files")
    void shouldSkipLoggingOfErrorsForEmptyFiles() {
        var fileSystemFacade = createFileSystemFacade(true,
                "/one.txt", "/two.txt", "empty.txt");

        var empty = workspace.toPath().resolve("empty.txt");
        when(fileSystemFacade.resolve(workspace, "empty.txt")).thenReturn(empty);
        when(fileSystemFacade.isEmpty(empty)).thenReturn(true);

        var scanner = new StringScanner(PATTERN, ENCODING, true, false,
                fileSystemFacade);

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);
        assertThat(actualResult.getResults()).containsExactly(CONTENT + 1, CONTENT + 2);
        assertThat(actualResult.getLog().getInfoMessages()).contains(
                "Searching for all files in '/absolute/path' that match the pattern '**/*.txt'",
                "-> found 3 files",
                "Successfully processed file '/one.txt'",
                "Successfully processed file '/two.txt'",
                "Skipping file 'empty.txt' because it's empty");
        assertThat(actualResult.hasErrors()).isFalse();
        assertThat(actualResult.getLog().getErrorMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should log error if no results are returned")
    void shouldLogErrorIfNoResultIsAvailable() {
        var fileSystemFacade = createFileSystemFacade(false, "/one.txt");

        var scanner = new EmptyScanner(fileSystemFacade);

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);
        assertThat(actualResult.getResults()).isEmpty();
        assertThat(actualResult.getLog().getInfoMessages()).containsExactly(
                "Searching for all files in '/absolute/path' that match the pattern '**/*.txt'",
                "Traversing of symbolic links: disabled",
                "-> found 1 file");
        assertThat(actualResult.hasErrors()).isTrue();
        assertThat(actualResult.getLog().getErrorMessages()).containsExactly("Errors during parsing",
                "No result created for file '/one.txt' due to some errors");
    }

    private FileSystemFacade createFileSystemFacade(final boolean followLinks, final String... files) {
        FileSystemFacade fileSystem = mock(FileSystemFacade.class);

        when(fileSystem.getAbsolutePath(any())).thenReturn("/absolute/path");
        when(fileSystem.find(PATTERN, followLinks, workspace)).thenReturn(files);

        return fileSystem;
    }

    @Override
    protected StringScanner createSerializable() {
        return new StringScanner(PATTERN, ENCODING, true, true, createFileSystemFacade(true));
    }

    static class StringScanner extends AgentFileVisitor<String> {
        private static final long serialVersionUID = -6902473746775046311L;
        private int counter = 1;

        @VisibleForTesting
        protected StringScanner(final String filePattern, final String encoding, final boolean followSymbolicLinks, final boolean errorOnEmptyFiles, final FileSystemFacade fileSystemFacade) {
            super(filePattern, encoding, followSymbolicLinks, errorOnEmptyFiles, fileSystemFacade);
        }

        @Override
        protected Optional<String> processFile(final Path file, final Charset charset, final FilteredLog log) {
            return Optional.of(CONTENT + counter++);
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @SuppressFBWarnings(value = "EQ_ALWAYS_TRUE", justification = "Required for serializable test")
        @Override
        public boolean equals(final Object o) {
            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static class EmptyScanner extends AgentFileVisitor<String> {
        private static final long serialVersionUID = 3700448215163706213L;

        @VisibleForTesting
        protected EmptyScanner(final FileSystemFacade fileSystemFacade) {
            super(PATTERN, ENCODING, false, false, fileSystemFacade);
        }

        @Override
        protected Optional<String> processFile(final Path file, final Charset charset, final FilteredLog log) {
            return Optional.empty();
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @SuppressFBWarnings(value = "EQ_ALWAYS_TRUE", justification = "Required for serializable test")
        @Override
        public boolean equals(final Object o) {
            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
