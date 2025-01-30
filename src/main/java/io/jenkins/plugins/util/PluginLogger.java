package io.jenkins.plugins.util;

import java.io.PrintStream;
import java.util.Collection;

import com.google.errorprone.annotations.FormatMethod;

/**
 * A simple logger that prefixes each message with the name of a plugin.
 *
 * @author Ullrich Hafner
 */
public class PluginLogger {
    private final String pluginName;
    private final PrintStream delegate;

    /**
     * Creates a new {@link PluginLogger}.
     *
     * @param logger
     *         the logger to create
     * @param pluginName
     *         the name of the plugin
     */
    public PluginLogger(final PrintStream logger, final String pluginName) {
        if (pluginName.contains("[")) {
            this.pluginName = pluginName + " ";
        }
        else {
            this.pluginName = "[%s] ".formatted(pluginName);
        }
        delegate = logger;
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
        print(format.formatted(args));
    }

    /**
     * Logs the specified messages.
     *
     * @param lines
     *         the messages to log
     */
    public void logEachLine(final Collection<String> lines) {
        lines.forEach(this::print);
    }

    private void print(final String line) {
        delegate.println(pluginName + line);
    }
}
