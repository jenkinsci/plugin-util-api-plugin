package io.jenkins.plugins.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import hudson.util.ComboBoxModel;

import static io.jenkins.plugins.util.CharsetValidation.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link CharsetValidation}.
 *
 * @author Arne Schöntag
 * @author Stephan Plöderl
 * @author Ullrich Hafner
 */
class CharsetValidationTest {
    @Test
    void shouldValidateCharsets() {
        CharsetValidation model = new CharsetValidation();

        FormValidationAssert.assertThat(model.validateCharset(""))
                .isOk();
        FormValidationAssert.assertThat(model.validateCharset("UTF-8"))
                .isOk();
        FormValidationAssert.assertThat(model.validateCharset("Some wrong text"))
                .isError()
                .hasMessage(createWrongEncodingErrorMessage());
    }

    @Test
    void shouldContainDefaultCharsets() {
        CharsetValidation model = new CharsetValidation();

        ComboBoxModel allCharsets = model.getAllCharsets();
        assertThat(allCharsets).isNotEmpty().contains("UTF-8", "ISO-8859-1");
    }

    @Test
    void shouldFallbackToPlatformCharset() {
        CharsetValidation model = new CharsetValidation();

        assertThat(model.getCharset("UTF-8")).isEqualTo(StandardCharsets.UTF_8);
        assertThat(model.getCharset("nothing")).isEqualTo(Charset.defaultCharset());
        assertThat(model.getCharset("")).isEqualTo(Charset.defaultCharset());
        assertThat(model.getCharset(null)).isEqualTo(Charset.defaultCharset());
    }
}
