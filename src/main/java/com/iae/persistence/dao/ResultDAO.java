package com.iae.persistence.dao;

import com.iae.domain.EvaluationResult;
import com.iae.domain.Status;
import com.iae.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultDAO {

    public void save(int projectId, EvaluationResult result) throws SQLException {

        String sql = """
            INSERT INTO evaluation_results
            (project_id, student_id, unzip_success, compile_success, run_success, output_match, error_log, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, projectId);
            stmt.setString(2, result.getStudentId());
            stmt.setBoolean(3, result.isUnzipSuccess());
            stmt.setBoolean(4, result.isCompileSuccess());
            stmt.setBoolean(5, result.isRunSuccess());
            stmt.setBoolean(6, result.isOutputMatch());
            stmt.setString(7, result.getErrorLog());
            stmt.setString(8, result.deriveStatus().name());

            stmt.executeUpdate();
        }
    }

    public List<EvaluationResult> findByProjectId(int projectId) throws SQLException {

        String sql = "SELECT * FROM evaluation_results WHERE project_id = ?";

        List<EvaluationResult> results = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, projectId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(map(rs));
                }
            }
        }

        return results;
    }

    public EvaluationResult findByProjectAndStudent(int projectId, String studentId) throws SQLException {

        String sql = """
            SELECT * FROM evaluation_results
            WHERE project_id = ? AND student_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, projectId);
            stmt.setString(2, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }

        return null;
    }

    public void deleteByProjectId(int projectId) throws SQLException {

        String sql = "DELETE FROM evaluation_results WHERE project_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, projectId);
            stmt.executeUpdate();
        }
    }

    private EvaluationResult map(ResultSet rs) throws SQLException {

        EvaluationResult result = new EvaluationResult(
                rs.getString("student_id")
        );

        result.setUnzipSuccess(rs.getBoolean("unzip_success"));
        result.setCompileSuccess(rs.getBoolean("compile_success"));
        result.setRunSuccess(rs.getBoolean("run_success"));
        result.setOutputMatch(rs.getBoolean("output_match"));
        result.setErrorLog(rs.getString("error_log"));

        // OPTIONAL (recommended consistency fix)
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                result.setStatus(Status.valueOf(statusStr));
            } catch (Exception ignored) {

            }
        }

        return result;
    }
}