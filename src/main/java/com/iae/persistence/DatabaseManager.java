package com.iae.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager
 *
 * <p>Manages the SQLite connection and schema initialisation for the IAE
 * application.  The database file is placed next to the running JAR as
 * {@code iae.db}.</p>
 *
 * <h2>Fixes applied</h2>
 * <ul>
 *   <li>Removed the single static {@code Connection} field.  Sharing one
 *       connection across the whole application is not safe: after the first
 *       {@code closeConnection()} call every subsequent {@code getConnection()}
 *       would try to reuse a closed object.  SQLite's JDBC driver is
 *       lightweight enough that opening a fresh connection per operation is
 *       perfectly fine.</li>
 *   <li>Schema is now loaded from the classpath resource
 *       {@code /database/schema.sql} instead of a raw filesystem path, so it
 *       works both in development and from the packaged JAR.</li>
 *   <li>WAL mode is enabled for better concurrent read performance.</li>
 * </ul>
 *
 * @author Dev 1
 * @version 1.1
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:iae.db";

    /** Utility class — do not instantiate. */
    private DatabaseManager() {}

    /**
     * Initialises the database: creates the file if it does not exist and
     * executes {@code schema.sql} so all tables are present.
     *
     * <p>Safe to call multiple times — all DDL statements use
     * {@code CREATE TABLE IF NOT EXISTS}.</p>
     *
     * @throws SQLException if the connection or DDL execution fails
     * @throws IOException  if the schema resource cannot be read
     */
    public static void initializeDatabase() throws SQLException, IOException {
        try (Connection conn = getConnection();
             Statement  stmt = conn.createStatement()) {

            // Enable WAL for better read concurrency
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA foreign_keys=ON;");

            String schema = loadSchema();
            // SQLite's JDBC driver does not support executeScript(), so split on ";"
            for (String ddl : schema.split(";")) {
                String trimmed = ddl.strip();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    /**
     * Opens and returns a new {@link Connection} to the SQLite database.
     *
     * <p>Callers are responsible for closing the connection (use
     * try-with-resources).</p>
     *
     * @return a fresh, open connection
     * @throws SQLException if the driver cannot open the database
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // Enforce FK constraints on every new connection
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys=ON;");
        }
        return conn;
    }



    /**
     * Reads {@code /database/schema.sql} from the classpath.
     */
    private static String loadSchema() throws IOException {
        try (InputStream is = DatabaseManager.class
                .getResourceAsStream("/database/schema.sql")) {

            if (is == null) {
                throw new IOException(
                        "schema.sql not found on classpath at /database/schema.sql");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}