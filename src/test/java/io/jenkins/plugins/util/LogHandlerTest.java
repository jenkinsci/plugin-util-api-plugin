package io.jenkins.plugins.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.hm.hafner.util.FilteredLog;

import hudson.model.TaskListener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link LogHandler}.
 *
 * @author Andreas Neumeier
 */
class LogHandlerTest {
    private static final String LOG_HANDLER_NAME = "TestHandler";
    private static final String MESSAGE = "TestMessage";
    private static final String NOT_SHOWN = "Not shown";
    private static final String ADDITIONAL_MESSAGE = "Additional";
    private static final String LOGGER_MESSAGE = "Logger message";

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "Log some messages and evaluate quiet flag value (quiet = {0})")
    void shouldLogInfoAndErrorMessage(final boolean quiet) {
        var outputStream = new ByteArrayOutputStream();
        var printStream = new PrintStream(outputStream);
        var taskListener = createTaskListener(printStream);

        var logger = new FilteredLog("Title");
        logger.logInfo(NOT_SHOWN);
        logger.logError(NOT_SHOWN);

        var logHandler = new LogHandler(taskListener, LOG_HANDLER_NAME, logger);
        logHandler.setQuiet(quiet);

        logger.logInfo(MESSAGE);
        logger.logError(MESSAGE);

        logHandler.log(logger);

        if (quiet) {
            assertThat(outputStream.toString()).isEmpty();
        }
        else {
            assertThat(outputStream).hasToString(String.format(
                    "[%s] [-ERROR-] %s%n"
                            + "[%s] %s%n",
                    LOG_HANDLER_NAME, MESSAGE, LOG_HANDLER_NAME, MESSAGE));
        }
        logger.logInfo(ADDITIONAL_MESSAGE);
        logger.logError(ADDITIONAL_MESSAGE);
        logHandler.log(logger);

        if (quiet) {
            assertThat(outputStream.toString()).isEmpty();
        }
        else {
            assertThat(outputStream).hasToString(String.format(
                    "[%s] [-ERROR-] %s%n"
                            + "[%s] %s%n"
                            + "[%s] [-ERROR-] %s%n"
                            + "[%s] %s%n",
                    LOG_HANDLER_NAME, MESSAGE, LOG_HANDLER_NAME, MESSAGE, LOG_HANDLER_NAME,
                    ADDITIONAL_MESSAGE, LOG_HANDLER_NAME, ADDITIONAL_MESSAGE));
        }
    }

    @Test
    void shouldLogFormattedMessage() {
        var outputStream = new ByteArrayOutputStream();
        var printStream = new PrintStream(outputStream);
        var taskListener = createTaskListener(printStream);
        var logHandler = new LogHandler(taskListener, LOG_HANDLER_NAME);

        logHandler.log(LOGGER_MESSAGE);

        assertThat(outputStream).hasToString("[%s] %s%n".formatted(LOG_HANDLER_NAME, LOGGER_MESSAGE));
    }

    private TaskListener createTaskListener(final PrintStream printStream) {
        TaskListener taskListener = mock(TaskListener.class);
        when(taskListener.getLogger()).thenReturn(printStream);
        return taskListener;
    }
}
