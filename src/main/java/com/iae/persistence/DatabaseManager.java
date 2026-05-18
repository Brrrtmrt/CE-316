package com.iae.persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DEFAULT_DB_URL;

    static {
        String userHome = System.getProperty("user.home");
        File appDir = new File(userHome, ".iae");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        DEFAULT_DB_URL = "jdbc:sqlite:" + new File(appDir, "iae.db").getAbsolutePath().replace("\\", "/");
    }

    private static String dbUrl = DEFAULT_DB_URL;
    private static boolean isInitialized = false;

    private DatabaseManager() {}

    public static synchronized void setDbUrl(String newUrl) {
        dbUrl = newUrl;
        isInitialized = false; // Reset flag so new test databases get auto-initialized
    }

    public static synchronized void resetDbUrl() {
        dbUrl = DEFAULT_DB_URL;
        isInitialized = false;
    }

    public static synchronized String getDbUrl() {
        return dbUrl;
    }

    public static synchronized Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);

        // Enable Foreign Key constraints for SQLite cascading deletes
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys=ON;");
        }

        // Auto-initialize schema if this database instance hasn't been configured yet
        if (!isInitialized) {
            isInitialized = true;
            try {
                initializeDatabase();
            } catch (Exception e) {
                System.err.println("WARNING: Auto-initialization failed: " + e.getMessage());
            }
        }
        return conn;
    }

    public static void initializeDatabase() throws SQLException, IOException {
        String schema = loadSchema();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            // Split by semicolon but carefully clean up dangling characters or comments
            String[] commands = schema.split(";");
            for (String query : commands) {
                String trimmed = query.trim();

                // If there's a trailing syntax error like a single hyphen '-', clean it safely
                if (trimmed.endsWith("-")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
                }

                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static String loadSchema() throws IOException {
        // Updated path to root resource folder directly
        String path = "/schema.sql";
        InputStream is = DatabaseManager.class.getResourceAsStream(path);
        if (is == null) {
            is = DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql");
        }
        if (is == null) {
            throw new IOException("Critical Error: schema.sql resource file missing from classpath root.");
        }

        try (InputStream autoCloseableIs = is) {
            return new String(autoCloseableIs.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}