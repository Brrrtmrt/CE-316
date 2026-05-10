package com.iae.persistence.;

import com.iae.domain.Project;
import com.iae.domain.Configuration;
import com.iae.evaluation.strategies.*;

import java.sql.*;

public class ProjectDAO {

    public Project findById(int id) throws SQLException {

        String sql = "SELECT * FROM projects WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    Configuration config = mapConfiguration(rs);

                    String args = rs.getString("program_arguments"); // if exists

                    return new Project(
                            config,
                            rs.getString("submissions_directory"),
                            args != null ? args.split(",") : new String[0],
                            rs.getString("expected_output")
                    );
                }
            }
        }

        return null;
    }

    private Configuration mapConfiguration(ResultSet rs) throws SQLException {

        String strategyStr = rs.getString("comparison_strategy");

        ComparisonStrategy strategy = switch (strategyStr) {
            case "IGNORE_WHITESPACE" -> new IgnoreWhitespaceStrategy();
            default -> new ExactMatchStrategy();
        };

        return new Configuration(
                rs.getString("config_name"),
                rs.getString("source_dir"),
                rs.getString("output_dir"),
                rs.getString("test_dir"),
                strategy,
                rs.getString("extra_param")
        );
    }
}