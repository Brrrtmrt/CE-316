package com.iae.persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class DatabaseManager {

    private static final String DEFAULT_DB_URL;

    static {
        String userHome = System.getProperty("user.home");
        File appDir = new File(userHome, ".iae");
        if (!appDir.exists() && !appDir.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create application directory: " + appDir.getAbsolutePath());
        }
        DEFAULT_DB_URL = "jdbc:sqlite:" + new File(appDir, "iae.db").getAbsolutePath().replace("\\", "/");
    }

    private static volatile String dbUrl = DEFAULT_DB_URL;
    private static volatile boolean isInitialized = false;
    private static final Object INIT_LOCK = new Object();

    private DatabaseManager() {}

    public static void setDbUrl(String newUrl) {
        if (newUrl == null || newUrl.isBlank()) {
            throw new IllegalArgumentException("dbUrl must not be null or blank");
        }
        if (!newUrl.startsWith("jdbc:sqlite:")) {
            throw new IllegalArgumentException("dbUrl must start with 'jdbc:sqlite:' but was: " + newUrl);
        }
        synchronized (INIT_LOCK) {
            dbUrl = newUrl;
            isInitialized = false;
        }
    }

    public static void resetDbUrl() {
        synchronized (INIT_LOCK) {
            dbUrl = DEFAULT_DB_URL;
            isInitialized = false;
        }
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static Connection getConnection() throws SQLException {
        ensureInitialized();
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys=ON;");
        }
        return conn;
    }

    private static void ensureInitialized() throws SQLException {
        if (isInitialized) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (isInitialized) {
                return;
            }
            try {
                initializeDatabase();
                isInitialized = true;
            } catch (IOException e) {
                throw new SQLException("Failed to initialize database schema", e);
            }
        }
    }

    public static void initializeDatabase() throws SQLException, IOException {
        String schema = stripSqlComments(loadSchema());

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys=ON;");

            for (String query : schema.split(";")) {
                String trimmed = query.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }

        migrateExistingDatabase();

        synchronized (INIT_LOCK) {
            isInitialized = true;
        }
    }

    private static void migrateExistingDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys=ON;");

            Set<String> projectsColumns = getColumnSet(stmt, "projects");
            if (!projectsColumns.contains("last_run_date")) {
                stmt.execute("ALTER TABLE projects ADD COLUMN last_run_date TEXT");
            }

            Set<String> resultsColumns = getColumnSet(stmt, "evaluation_results");
            if (!resultsColumns.contains("program_output")) {
                stmt.execute("ALTER TABLE evaluation_results ADD COLUMN program_output TEXT");
            }

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='evaluation_results'")) {
                if (rs.next()) {
                    try (ResultSet idxRs = stmt.executeQuery(
                            "SELECT name FROM sqlite_master WHERE type='index' "
                            + "AND tbl_name='evaluation_results' AND sql LIKE '%UNIQUE%'")) {
                        if (!idxRs.next()) {
                            stmt.execute(
                                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_eval_results_project_student "
                                    + "ON evaluation_results(project_id, student_id)");
                        }
                    }
                }
            }
        }
    }

    private static Set<String> getColumnSet(Statement stmt, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        }
        return columns;
    }

    private static String stripSqlComments(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        for (String line : sql.split("\\r?\\n")) {
            int idx = line.indexOf("--");
            if (idx >= 0) {
                line = line.substring(0, idx);
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static String loadSchema() throws IOException {
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
