package io.jenkins.plugins.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;

/**
 * Validates UI parameters. For the validated UI elements, also additional utility methods are provided.
 *
 * @author Ullrich Hafner
 */
public class ValidationUtilities {
    private static final Set<String> ALL_CHARSETS = Charset.availableCharsets().keySet();
    private static final Pattern VALID_ID_PATTERN = Pattern.compile("\\p{Alnum}[\\p{Alnum}-_.]*");

    /**
     * Returns all available character set names.
     *
     * @return all available character set names
     */
    public ComboBoxModel getAllCharsets() {
        return new ComboBoxModel(ALL_CHARSETS);
    }

    /**
     * Returns the default charset for the specified encoding string. If the default encoding is empty or {@code null},
     * or if the charset is not valid then the default encoding of the platform is returned.
     *
     * @param charset
     *         identifier of the character set
     *
     * @return the default charset for the specified encoding string
     */
    public Charset getCharset(@CheckForNull final String charset) {
        try {
            if (StringUtils.isNotBlank(charset)) {
                return Charset.forName(charset);
            }
        }
        catch (UnsupportedCharsetException | IllegalCharsetNameException exception) {
            // ignore and return default
        }
        return Charset.defaultCharset();
    }

    /**
     * Performs on-the-fly validation of the character encoding.
     *
     * @param reportEncoding
     *         the character encoding
     *
     * @return the validation result
     */
    public FormValidation validateCharset(final String reportEncoding) {
        try {
            if (StringUtils.isBlank(reportEncoding) || Charset.isSupported(reportEncoding)) {
                return FormValidation.ok();
            }
        }
        catch (IllegalCharsetNameException | UnsupportedCharsetException ignore) {
            // throw a FormValidation error
        }
        return FormValidation.errorWithMarkup(createWrongEncodingErrorMessage());
    }

    @VisibleForTesting
    static String createWrongEncodingErrorMessage() {
        return Messages.FieldValidator_Error_DefaultEncoding(
                "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html");
    }

    /**
     * Ensures that the specified ID is valid.
     *
     * @param id
     *         the custom ID of the tool
     *
     * @throws IllegalArgumentException if the ID is not valid
     */
    public void ensureValidId(final String id) {
        if (!isValidId(id)) {
            throw new IllegalArgumentException(createInvalidIdMessage(id));
        }
    }

    /**
     * Performs on-the-fly validation of the ID.
     *
     * @param id
     *         the custom ID of the tool
     *
     * @return the validation result
     */
    public FormValidation validateId(final String id) {
        if (isValidId(id)) {
            return FormValidation.ok();
        }
        return FormValidation.error(createInvalidIdMessage(id));
    }

    private boolean isValidId(final String id) {
        return StringUtils.isEmpty(id) || VALID_ID_PATTERN.matcher(id).matches();
    }

    static String createInvalidIdMessage(final String id) {
        return Messages.FieldValidator_Error_WrongIdFormat(VALID_ID_PATTERN.pattern(), id);
    }

    /**
     * Performs on-the-fly validation on the ant pattern for input files.
     *
     * @param project
     *         the project that is configured
     * @param pattern
     *         the file pattern
     *
     * @return the validation result
     */
    public FormValidation doCheckPattern(final AbstractProject<?, ?> project, final String pattern) {
        if (project != null) { // there is no workspace in pipelines
            try {
                var workspace = project.getSomeWorkspace();
                if (workspace != null && workspace.exists()) {
                    return validatePatternInWorkspace(pattern, workspace);
                }
            }
            catch (InterruptedException | IOException ignore) {
                // ignore and return ok
            }
        }

        return FormValidation.ok();
    }

    private FormValidation validatePatternInWorkspace(final String pattern, final FilePath workspace)
            throws IOException, InterruptedException {
        var result = workspace.validateAntFileMask(pattern, FilePath.VALIDATE_ANT_FILE_MASK_BOUND);
        if (result != null) {
            return FormValidation.error(result);
        }
        return FormValidation.ok();
    }
}
