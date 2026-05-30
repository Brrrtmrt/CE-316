package com.iae.util;

import java.io.File;

/**
 * Application-wide constants for the Integrated Assignment Environment (IAE).
 *
 * <p>This class centralises the "magic" strings and paths that would otherwise
 * be duplicated acroass the persistence, service and GUI layers (for example the
 * {@code ~/.iae} application directory, the {@code config} sub-directory, the
 * SQLite database file name and the JSON field keys). Keeping them in one place
 * prevents the layers from drifting apart over time.
 *
 * <p>The class is non-instantiable: it only exposes {@code public static final}
 * values and a few small path helpers.
 */
public final class Constants {

    private Constants() {
        // Utility class - must not be instantiated.
        throw new AssertionError("Constants is a utility class and cannot be instantiated.");
    }

    // -------------------------------------------------------------------------
    // Application metadata
    // -------------------------------------------------------------------------

    /** Human-readable application name. */
    public static final String APP_NAME = "Integrated Assignment Environment";

    /** Short application name, suitable for window titles and log lines. */
    public static final String APP_SHORT_NAME = "IAE";

    /** Application version (kept in sync with the {@code <version>} in pom.xml). */
    public static final String APP_VERSION = "1.0-SNAPSHOT";

    // -------------------------------------------------------------------------
    // File system layout (all relative to the user's home directory)
    // -------------------------------------------------------------------------

    /** Name of the per-user application directory created under {@code user.home}. */
    public static final String APP_DIR_NAME = ".iae";

    /** Name of the directory (inside {@link #APP_DIR_NAME}) that stores configuration JSON files. */
    public static final String CONFIG_DIR_NAME = "config";

    /** Name of the directory (inside {@link #APP_DIR_NAME}) that stores log files. */
    public static final String LOG_DIR_NAME = "logs";

    /** File name of the SQLite database. */
    public static final String DATABASE_FILE_NAME = "iae.db";

    /** File name of the main application log. */
    public static final String LOG_FILE_NAME = "iae.log";

    /** JDBC URL scheme expected by {@code DatabaseManager}. */
    public static final String SQLITE_URL_PREFIX = "jdbc:sqlite:";

    // -------------------------------------------------------------------------
    // File extensions
    // -------------------------------------------------------------------------

    /** Extension (including the leading dot) used for configuration files. */
    public static final String CONFIG_FILE_EXTENSION = ".json";

    /** Glob pattern that matches configuration files in a directory listing. */
    public static final String CONFIG_FILE_GLOB = "*" + CONFIG_FILE_EXTENSION;

    // -------------------------------------------------------------------------
    // JSON field keys for a serialised Configuration
    // (must match the keys read/written by ConfigurationIO)
    // -------------------------------------------------------------------------

    public static final String KEY_NAME              = "name";
    public static final String KEY_LANGUAGE          = "language";
    public static final String KEY_FILE_EXTENSION    = "fileExtension";
    public static final String KEY_COMPILE_COMMAND   = "compileCommand";
    public static final String KEY_RUN_COMMAND       = "runCommand";
    public static final String KEY_COMPARISON_METHOD = "comparisonMethod";
    public static final String KEY_DESCRIPTION       = "description";

    // -------------------------------------------------------------------------
    // Comparison-strategy keys (must match ConfigurationIO.resolveStrategy)
    // -------------------------------------------------------------------------

    public static final String COMPARISON_EXACT             = "exact";
    public static final String COMPARISON_IGNORE_WHITESPACE = "ignore_whitespace";
    public static final String COMPARISON_TRIM_LINES        = "trim_lines";

    /** The comparison method assumed when a configuration file omits the key. */
    public static final String DEFAULT_COMPARISON_METHOD = COMPARISON_EXACT;

    // -------------------------------------------------------------------------
    // Command placeholders substituted before a command is executed
    // -------------------------------------------------------------------------

    /** Placeholder for the source file path in compile/run commands. */
    public static final String PLACEHOLDER_SOURCE = "{src}";

    /** Placeholder for the compiled output path in compile/run commands. */
    public static final String PLACEHOLDER_OUTPUT = "{out}";

    /** Placeholder for program arguments in run commands. */
    public static final String PLACEHOLDER_ARGS = "{args}";

    // -------------------------------------------------------------------------
    // Path helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the per-user application directory ({@code ~/.iae}) as a {@link File}.
     * The directory is <em>not</em> created by this call.
     *
     * @return the application directory
     */
    public static File getAppDir() {
        return new File(System.getProperty("user.home"), APP_DIR_NAME);
    }

    /**
     * Returns the configuration directory ({@code ~/.iae/config}) as a {@link File}.
     * The directory is <em>not</em> created by this call.
     *
     * @return the configuration directory
     */
    public static File getConfigDir() {
        return new File(getAppDir(), CONFIG_DIR_NAME);
    }

    /**
     * Returns the log directory ({@code ~/.iae/logs}) as a {@link File}.
     * The directory is <em>not</em> created by this call.
     *
     * @return the log directory
     */
    public static File getLogDir() {
        return new File(getAppDir(), LOG_DIR_NAME);
    }

    /**
     * Returns the SQLite database file ({@code ~/.iae/iae.db}) as a {@link File}.
     *
     * @return the database file
     */
    public static File getDatabaseFile() {
        return new File(getAppDir(), DATABASE_FILE_NAME);
    }
}
