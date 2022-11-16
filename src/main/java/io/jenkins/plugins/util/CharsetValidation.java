package io.jenkins.plugins.util;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;

/**
 * Validates the charset configuration.
 *
 * @author Ullrich Hafner
 */
public class CharsetValidation {
    private static final Set<String> ALL_CHARSETS = Charset.availableCharsets().keySet();

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
                "https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html");
    }
}
