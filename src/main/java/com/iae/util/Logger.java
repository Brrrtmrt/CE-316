package com.iae.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

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

    private static final java.util.logging.Logger ROOT =
            java.util.logging.Logger.getLogger("com.iae");

    private static volatile boolean initialised = false;

    private Logger() {
        throw new AssertionError("Logger is a utility class and cannot be instantiated.");
    }

    static {
        init();
    }

    // Fix: removed "exposed package-private" claim from comment since method is private (Copilot)
    private static synchronized void init() {
        if (initialised) {
            return;
        }

        ROOT.setUseParentHandlers(false);
        ROOT.setLevel(Level.ALL);

        Formatter formatter = new IaeFormatter();

        Handler console = new java.util.logging.ConsoleHandler();
        console.setLevel(Level.INFO);
        console.setFormatter(formatter);
        ROOT.addHandler(console);

        try {
            File logDir = Constants.getLogDir();
            if (!logDir.exists() && !logDir.mkdirs()) {
                throw new IOException("Could not create log directory: " + logDir);
            }
            String pattern = new File(logDir, Constants.LOG_FILE_NAME).getAbsolutePath();

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

    public static void debug(String message) {
        ROOT.log(Level.FINE, message);
    }

    public static void info(String message) {
        ROOT.log(Level.INFO, message);
    }

    public static void warn(String message) {
        ROOT.log(Level.WARNING, message);
    }

    public static void error(String message) {
        ROOT.log(Level.SEVERE, message);
    }

    public static void error(String message, Throwable throwable) {
        ROOT.log(Level.SEVERE, message, throwable);
    }

    public static java.util.logging.Logger forName(String name) {
        java.util.logging.Logger child = java.util.logging.Logger.getLogger(name);
        child.setParent(ROOT);
        return child;
    }

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