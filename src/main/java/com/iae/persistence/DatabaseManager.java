package com.iae.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:iae.db";
    private static Connection connection;

    public static void initializeDatabase() throws SQLException, IOException {

        connection = DriverManager.getConnection(DB_URL);

        String schema = Files.readString(Path.of("schema.sql"));

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(schema);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    public static void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}