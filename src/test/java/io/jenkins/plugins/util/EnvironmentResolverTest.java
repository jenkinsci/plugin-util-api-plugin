package io.jenkins.plugins.util;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import hudson.EnvVars;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit test for {@link EnvironmentResolver}.
 *
 * @author Deniz Mardin
 * @author Frank Christian Geyer
 */
class EnvironmentResolverTest {
    private static final int NUMBER_OF_CHARS = 1024;
    private static final String KEY = "KEY";
    private static final String VALUE = "VALUE";
    private static final String RESULT = "RESULT";

    /**
     * Verifies that the output is empty for an empty input.
     */
    @Test
    void shouldReturnUnmodifiedOutputForEmptyInput() {
        var environmentResolver = new EnvironmentResolver();
        var expanded = environmentResolver.expandEnvironmentVariables(new EnvVars(), "");

        assertThat(expanded).isEmpty();
    }

    /**
     * Verifies that the output is the same for a conventional input.
     */
    @Test
    void shouldReturnSameOutputAsInput() {
        var environmentResolver = new EnvironmentResolver();
        var expanded = environmentResolver.expandEnvironmentVariables(new EnvVars(), "TestString");

        assertThat(expanded).isEqualTo("TestString");
    }

    /**
     * Verifies that the output is the same as the input for an environment which is set to null.
     */
    @Test
    void shouldReturnSameOutputAsInputForAnEnvironmentWhichIsNull() {
        var environmentResolver = new EnvironmentResolver();
        var expanded = environmentResolver.expandEnvironmentVariables(null, "TestStringForNull");

        assertThat(expanded).isEqualTo("TestStringForNull");
    }

    /**
     * Verifies that the output is the same for a given input without further adjustments.
     */
    @Test
    void shouldReturnNonAdjustedOutputForDefinedInput() {
        var environmentResolver = new EnvironmentResolver();
        var envVars = new EnvVars();
        envVars.put(KEY, VALUE);
        var expected = "TestTesting";
        var expanded = environmentResolver.expandEnvironmentVariables(envVars, expected);

        assertThat(expanded).isEqualTo(expected);
    }

    /**
     * Verifies that the output is adjusted correctly for a given input.
     */
    @Test
    void shouldReturnAdjustedOutputForDefinedInput() {
        var environmentResolver = new EnvironmentResolver();
        var envVars = new EnvVars();
        envVars.put(KEY, VALUE);
        var previous = "$" + KEY;
        var expanded = environmentResolver.expandEnvironmentVariables(envVars, previous);

        assertThat(expanded).isEqualTo(VALUE);
    }

    /**
     * Verifies that the output is adjusted correctly for a given input two times in a row.
     */
    @Test
    void shouldReplaceTheFirstKeyWithFirstValueAndThenReplaceFirstValueWithResult() {
        var environmentResolver = new EnvironmentResolver();
        var envVars = new EnvVars();
        envVars.put(KEY, "$" + VALUE);
        envVars.put(VALUE, RESULT);
        var previous = "$" + KEY;
        var expanded = environmentResolver.expandEnvironmentVariables(envVars, previous);

        assertThat(expanded).isEqualTo(RESULT);
    }

    /**
     * Verifies that the output is an adjusted value of a given special input, which is based on $-signs.
     * This is rather a test of the method replaceMacro in the class {@link hudson.Util}.
     */
    @Test
    void shouldReturnAdjustedOutputForAGivenInputBasedOnDollarSigns() {
        var environmentResolver = new EnvironmentResolver();
        var envVars = new EnvVars();
        envVars.put(KEY, VALUE);
        var previous = "$$Test$$Testing$TestString$$Testing$Test";
        var expanded = environmentResolver.expandEnvironmentVariables(envVars, previous);
        var expected = "$Test$Testing$TestString$Testing$Test";

        assertThat(expanded).isEqualTo(expected);
    }

    /**
     * Verifies that the loop is executed zero times.
     */
    @Test
    void shouldRunZeroTimesThroughTheLoopAndLeaveTheInputUnmodified() {
        var envVars = new EnvVars();
        envVars.put(KEY, VALUE);
        var nonExpanded = "$";
        var expected = "$";

        checkLoopWithNumberOfRuns(0, envVars, nonExpanded, expected);
    }

    /**
     * Verifies that the loop is executed one time.
     */
    @Test
    void shouldRunOneTimeThroughTheLoopAndModifyTheInput() {
        var envVars = new EnvVars();
        envVars.put(KEY, VALUE);
        var nonExpanded = "$$";
        var expected = "$";

        checkLoopWithNumberOfRuns(1, envVars, nonExpanded, expected);
    }

    /**
     * Verifies that the loop gets exited after the statical set retries {@code RESOLVE_VARIABLES_DEPTH} successfully
     * and the second condition StringUtils.isNotBlank(expanded) is fulfilled.
     */
    @Test
    void shouldExitAfterSetRetriesHasExceededAndHenceLeaveTheInputUnmodified() {
        var environmentResolver = new EnvironmentResolver();
        var envVars = new EnvVars();
        envVars.put(KEY, VALUE);
        var expanded = environmentResolver.expandEnvironmentVariables(envVars, createDollarString());

        assertThat(expanded).isEqualTo("$");
    }

    /**
     * Verifies that the loop gets exited after the second condition StringUtils.isNotBlank(expanded) is not fulfilled.
     */
    @Test
    void shouldExitAfterSetRetriesHasExceededAndHenceLeaveTheInputUnmodifiedTestTheSecondShortCircuitCondition() {
        var environmentResolver = new EnvironmentResolver();
        var envVars = new EnvVars();
        envVars.put(KEY, VALUE);
        var expanded = environmentResolver.expandEnvironmentVariables(envVars, " ");

        assertThat(expanded).isBlank();
    }

    private String createDollarString() {
        char[] chars = new char[NUMBER_OF_CHARS];
        Arrays.fill(chars, '$');
        return new String(chars);
    }

    private void checkLoopWithNumberOfRuns(final int resolveVariablesDepth, final EnvVars environment,
            final String nonExpanded, final String expected) {
        var environmentResolver = new EnvironmentResolver(resolveVariablesDepth);
        var expanded = environmentResolver.expandEnvironmentVariables(environment, nonExpanded);

        assertThat(expanded).isEqualTo(expected);
    }
}
