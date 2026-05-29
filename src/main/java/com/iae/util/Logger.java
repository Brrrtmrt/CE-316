package com.iae.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Lightweight, application-wide logging facade for IAE.
 *
 * <p>It wraps the JDK's {@link java.util.logging} framework and configures it
 * once, the first time the class is loaded, so that every log message is sent
 * to two destinations:
 * <ul>
 *   <li>the console ({@code System.err}), for immediate feedback during development; and</li>
 *   <li>a rotating file under {@code ~/.iae/logs/iae.log}, so problems reported by
 *       users can be diagnosed after the fact.</li>
 * </ul>
 *
 * <p>Typical usage is through the static convenience methods:
 * <pre>{@code
 *     Logger.info("Loaded " + count + " configuration(s).");
 *     Logger.error("Failed to import configuration", exception);
 * }</pre>
 *
 * <p>If the log file cannot be created (for example because the home directory
 * is read-only), logging silently falls back to console-only mode rather than
 * crashing the application.
 */
public final class Logger {

    /** Single root logger that all messages flow through. */
    private static final java.util.logging.Logger ROOT =
            java.util.logging.Logger.getLogger("com.iae");

    private static volatile boolean initialised = false;

    private Logger() {
        // Utility class - must not be instantiated.
        throw new AssertionError("Logger is a utility class and cannot be instantiated.");
    }

    static {
        init();
    }

    /**
     * Configures the underlying JDK logger exactly once. Subsequent calls are
     * no-ops. This is invoked automatically from the static initialiser, but is
     * exposed (package-private) so tests can trigger configuration explicitly.
     */
    private static synchronized void init() {
        if (initialised) {
            return;
        }

        // Detach from the parent handlers so we fully control the output format
        // and do not get duplicate console lines from the global root logger.
        ROOT.setUseParentHandlers(false);
        ROOT.setLevel(Level.ALL);

        Formatter formatter = new IaeFormatter();

        // Console handler (warnings and above are the common case during runs,
        // but we keep INFO visible because the existing code logs progress at INFO).
        Handler console = new java.util.logging.ConsoleHandler();
        console.setLevel(Level.INFO);
        console.setFormatter(formatter);
        ROOT.addHandler(console);

        // File handler under ~/.iae/logs. Best-effort: console-only on failure.
        try {
            File logDir = Constants.getLogDir();
            if (!logDir.exists() && !logDir.mkdirs()) {
                throw new IOException("Could not create log directory: " + logDir);
            }
            String pattern = new File(logDir, Constants.LOG_FILE_NAME).getAbsolutePath();

            // 1 MB per file, keep 3 rotated files, append on restart.
            FileHandler file = new FileHandler(pattern, 1_000_000, 3, true);
            file.setLevel(Level.ALL);
            file.setFormatter(formatter);
            ROOT.addHandler(file);
        } catch (IOException | SecurityException e) {
            ROOT.log(Level.WARNING,
                    "File logging disabled (console only): " + e.getMessage());
        }

        initialised = true;
    }

    // -------------------------------------------------------------------------
    // Convenience API
    // -------------------------------------------------------------------------

    /** Logs a fine-grained debugging message. */
    public static void debug(String message) {
        ROOT.log(Level.FINE, message);
    }

    /** Logs an informational message about normal application progress. */
    public static void info(String message) {
        ROOT.log(Level.INFO, message);
    }

    /** Logs a recoverable problem that the user should be aware of. */
    public static void warn(String message) {
        ROOT.log(Level.WARNING, message);
    }

    /** Logs an error. */
    public static void error(String message) {
        ROOT.log(Level.SEVERE, message);
    }

    /**
     * Logs an error together with the throwable that caused it. The stack trace
     * is written to the log file so it can be inspected later.
     *
     * @param message a description of what was being attempted
     * @param throwable the exception that was caught (may be {@code null})
     */
    public static void error(String message, Throwable throwable) {
        ROOT.log(Level.SEVERE, message, throwable);
    }

    /**
     * Returns a named child logger, useful when a class wants its own logger
     * name to appear in the output (for example {@code "com.iae.service.X"}).
     * The returned logger inherits IAE's handlers and formatting.
     *
     * @param name the logger name, typically {@code Foo.class.getName()}
     * @return a configured {@link java.util.logging.Logger}
     */
    public static java.util.logging.Logger forName(String name) {
        // Touching ROOT guarantees init() has run before the child is handed out.
        java.util.logging.Logger child = java.util.logging.Logger.getLogger(name);
        child.setParent(ROOT);
        return child;
    }

    /**
     * Compact single-line formatter: {@code [yyyy-MM-dd HH:mm:ss] LEVEL message}.
     */
    private static final class IaeFormatter extends Formatter {

        private static final java.time.format.DateTimeFormatter TS =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            String timestamp = java.time.LocalDateTime
                    .ofInstant(record.getInstant(), java.time.ZoneId.systemDefault())
                    .format(TS);

            sb.append('[').append(timestamp).append("] ")
              .append(record.getLevel().getName()).append(' ')
              .append(formatMessage(record))
              .append(System.lineSeparator());

            if (record.getThrown() != null) {
                java.io.StringWriter sw = new java.io.StringWriter();
                record.getThrown().printStackTrace(new java.io.PrintWriter(sw));
                sb.append(sw);
            }
            return sb.toString();
        }
    }
}
