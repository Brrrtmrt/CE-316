package com.iae.persistence.dao;

import com.iae.domain.EvaluationResult;
import com.iae.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ResultDAO extends BaseDAO {

    private static final String SELECT_COLUMNS =
            "id, project_id, student_id, unzip_success, compile_success, run_success, "
            + "output_match, error_log, program_output, status";

    public List<EvaluationResult> findByProjectId(String projectId) throws SQLException {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM evaluation_results WHERE project_id = ? ORDER BY student_id";
        List<EvaluationResult> results = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(projectId));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResult(rs));
                }
            }
        }

        return results;
    }


    public EvaluationResult findByProjectAndStudent(String projectId, String studentId) throws SQLException {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("studentId must not be null");
        }
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM evaluation_results WHERE project_id = ? AND student_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(projectId));
            stmt.setString(2, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResult(rs);
                }
            }
        }

        return null;
    }


    public void save(String projectId, EvaluationResult result) throws SQLException {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }

        String deleteSql = "DELETE FROM evaluation_results WHERE project_id = ? AND student_id = ?";
        String insertSql = """
                INSERT INTO evaluation_results
                    (project_id, student_id,
                     unzip_success, compile_success, run_success, output_match,
                     error_log, program_output, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int projectIdInt = Integer.parseInt(projectId);

        try (Connection conn = DatabaseManager.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                    delStmt.setInt(1, projectIdInt);
                    delStmt.setString(2, result.getStudentId());
                    delStmt.executeUpdate();
                }

                try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                    insStmt.setInt(1,    projectIdInt);
                    insStmt.setString(2, result.getStudentId());
                    insStmt.setInt(3,    result.isUnzipSuccess()   ? 1 : 0);
                    insStmt.setInt(4,    result.isCompileSuccess()  ? 1 : 0);
                    insStmt.setInt(5,    result.isRunSuccess()      ? 1 : 0);
                    insStmt.setInt(6,    result.isOutputMatch()     ? 1 : 0);
                    insStmt.setString(7, result.getErrorLog());
                    insStmt.setString(8, result.getProgramOutput());
                    insStmt.setString(9, result.getStatus().name());

                    insStmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        }
    }


    public void deleteByProjectId(String projectId) throws SQLException {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        String sql = "DELETE FROM evaluation_results WHERE project_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(projectId));
            stmt.executeUpdate();
        }
    }


    private EvaluationResult mapResult(ResultSet rs) throws SQLException {
        EvaluationResult result = new EvaluationResult(rs.getString("student_id"));
        result.setUnzipSuccess(rs.getInt("unzip_success")     == 1);
        result.setCompileSuccess(rs.getInt("compile_success") == 1);
        result.setRunSuccess(rs.getInt("run_success")         == 1);
        result.setOutputMatch(rs.getInt("output_match")       == 1);
        result.setErrorLog(rs.getString("error_log"));
        result.setProgramOutput(rs.getString("program_output"));
        return result;
    }
}
