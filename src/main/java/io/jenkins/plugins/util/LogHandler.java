package io.jenkins.plugins.util;

import java.util.List;

import com.google.errorprone.annotations.FormatMethod;

import edu.hm.hafner.util.FilteredLog;

import hudson.model.TaskListener;

/**
 * Handles logging of issues log and error messages to a {@link TaskListener} instance.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.LoggerIsNotStaticFinal")
public class LogHandler {
    private final PluginLogger errorLogger;
    private final PluginLogger infoLogger;

    private int infoPosition;
    private int errorPosition;
    private boolean quiet = false;

    /**
     * Creates a new {@link LogHandler}.
     *
     * @param listener
     *         the task listener that will print all log messages
     * @param name
     *         the name of the logger
     */
    public LogHandler(final TaskListener listener, final String name) {
        this(listener, name, 0, 0);
    }

    /**
     * Creates a new {@link LogHandler}.
     *
     * @param listener
     *         the task listener that will print all log messages
     * @param name
     *         the name of the logger
     * @param logger
     *         the logger that contains the actual log messages
     */
    public LogHandler(final TaskListener listener, final String name, final FilteredLog logger) {
        this(listener, name, logger.getInfoMessages().size(), logger.getErrorMessages().size());
    }

    private LogHandler(final TaskListener listener, final String name, final int infoPosition,
            final int errorPosition) {
        infoLogger = createLogger(listener, name);
        errorLogger = createErrorLogger(listener, name);
        this.infoPosition = infoPosition;
        this.errorPosition = errorPosition;
    }

    private PluginLogger createErrorLogger(final TaskListener listener, final String name) {
        return createLogger(listener, String.format("[%s] [-ERROR-]", name));
    }

    private PluginLogger createLogger(final TaskListener listener, final String name) {
        return new PluginLogger(listener.getLogger(), name);
    }

    /**
     * Log all info and error messages that are stored in the set of issues. Note that subsequent calls to this method
     * will only log messages that have not yet been logged.
     *
     * @param logger
     *         the logger with the collected messages
     */
    public void log(final FilteredLog logger) {
        logErrorMessages(logger);
        logInfoMessages(logger);
    }

    /**
     * Logs the specified message.
     *
     * @param format
     *         A <a href="../util/Formatter.html#syntax">format string</a>
     * @param args
     *         Arguments referenced by the format specifiers in the format string.  If there are more arguments than
     *         format specifiers, the extra arguments are ignored.  The number of arguments is variable and may be
     *         zero.
     */
    @FormatMethod
    public void log(final String format, final Object... args) {
        infoLogger.log(format, args);
    }

    // TODO: extract to method
    private void logErrorMessages(final FilteredLog logger) {
        List<String> errorMessages = logger.getErrorMessages();
        if (errorPosition < errorMessages.size() && !quiet) {
            errorLogger.logEachLine(errorMessages.subList(errorPosition, errorMessages.size()));
            errorPosition = errorMessages.size();
        }
    }

    private void logInfoMessages(final FilteredLog logger) {
        List<String> infoMessages = logger.getInfoMessages();
        if (infoPosition < infoMessages.size() && !quiet) {
            infoLogger.logEachLine(infoMessages.subList(infoPosition, infoMessages.size()));
            infoPosition = infoMessages.size();
        }
    }

    public void setQuiet(final boolean quiet) {
        this.quiet = quiet;
    }
}
