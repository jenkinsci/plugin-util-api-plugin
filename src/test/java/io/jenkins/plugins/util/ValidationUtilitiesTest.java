package io.jenkins.plugins.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import hudson.util.ComboBoxModel;

import static io.jenkins.plugins.util.FormValidationAssert.assertThat;
import static io.jenkins.plugins.util.ValidationUtilities.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link ValidationUtilities}.
 *
 * @author Arne Schöntag
 * @author Stephan Plöderl
 * @author Ullrich Hafner
 */
class ValidationUtilitiesTest {
    @Test
    void shouldValidateCharsets() {
        ValidationUtilities model = new ValidationUtilities();

        assertThat(model.validateCharset("")).isOk();
        assertThat(model.validateCharset("UTF-8")).isOk();
        assertThat(model.validateCharset("Some wrong text"))
                .isError()
                .hasMessage(createWrongEncodingErrorMessage());
    }

    @Test
    void shouldContainDefaultCharsets() {
        ValidationUtilities model = new ValidationUtilities();

        ComboBoxModel allCharsets = model.getAllCharsets();
        assertThat(allCharsets).isNotEmpty().contains("UTF-8", "ISO-8859-1");
    }

    @Test
    void shouldFallbackToPlatformCharset() {
        ValidationUtilities model = new ValidationUtilities();

        assertThat(model.getCharset("UTF-8")).isEqualTo(StandardCharsets.UTF_8);
        assertThat(model.getCharset("nothing")).isEqualTo(Charset.defaultCharset());
        assertThat(model.getCharset("")).isEqualTo(Charset.defaultCharset());
        assertThat(model.getCharset(null)).isEqualTo(Charset.defaultCharset());
    }

    @ParameterizedTest(name = "{index} => Should be marked as illegal ID: \"{0}\"")
    @ValueSource(strings = {"a b", "a/b", "a#b", "äöü", "aö", ".", ".."})
    @DisplayName("should reject IDs")
    void shouldRejectId(final String id) {
        ValidationUtilities model = new ValidationUtilities();

        assertThat(model.validateId(id)).isError().hasMessage(createInvalidIdMessage(id));
        assertThatIllegalArgumentException().isThrownBy(() -> model.ensureValidId(id));
    }

    @ParameterizedTest(name = "{index} => Should be marked as valid ID: \"{0}\"")
    @ValueSource(strings = {"", "a", "awordb", "a-b", "a_b", "0", "a0b", "12a34", "A", "aBc", "a.b-c_e", "z.."})
    @DisplayName("should accept IDs")
    void shouldAcceptId(final String id) {
        ValidationUtilities model = new ValidationUtilities();

        assertThat(model.validateId(id)).isOk();
        assertThatCode(() -> model.ensureValidId(id)).doesNotThrowAnyException();
    }
}
