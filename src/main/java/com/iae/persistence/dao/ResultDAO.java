package com.iae.persistence.dao;

import com.iae.domain.EvaluationResult;
import com.iae.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ResultDAO extends BaseDAO {


    public List<EvaluationResult> findByProjectId(String projectId) {
        String sql = "SELECT * FROM evaluation_results WHERE project_id = ?";
        List<EvaluationResult> results = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(projectId));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResult(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("ResultDAO.findByProjectId failed: " + e.getMessage());
        }

        return results;
    }


    public EvaluationResult findByProjectAndStudent(String projectId, String studentId) {
        String sql = """
                SELECT * FROM evaluation_results
                WHERE project_id = ? AND student_id = ?
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(projectId));
            stmt.setString(2, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResult(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("ResultDAO.findByProjectAndStudent failed: " + e.getMessage());
        }

        return null;
    }


    public void save(String projectId, EvaluationResult result) throws SQLException {
        String deleteSql = "DELETE FROM evaluation_results WHERE project_id = ? AND student_id = ?";
        String insertSql = """
                INSERT INTO evaluation_results
                    (project_id, student_id,
                     unzip_success, compile_success, run_success, output_match,
                     error_log, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getConnection()) {

            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                delStmt.setInt(1, Integer.parseInt(projectId));
                delStmt.setString(2, result.getStudentId());
                delStmt.executeUpdate();
            }

            try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                insStmt.setInt(1,    Integer.parseInt(projectId));
                insStmt.setString(2, result.getStudentId());
                insStmt.setInt(3,    result.isUnzipSuccess()   ? 1 : 0);
                insStmt.setInt(4,    result.isCompileSuccess()  ? 1 : 0);
                insStmt.setInt(5,    result.isRunSuccess()      ? 1 : 0);
                insStmt.setInt(6,    result.isOutputMatch()     ? 1 : 0);
                insStmt.setString(7, result.getErrorLog());

                String statusStr = (result.getStatus() != null) ? result.getStatus().name() : "COMPLETED";
                insStmt.setString(8, statusStr);

                insStmt.executeUpdate();
            }
        }
    }


    public void deleteByProjectId(String projectId) {
        String sql = "DELETE FROM evaluation_results WHERE project_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(projectId));
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("ResultDAO.deleteByProjectId failed: " + e.getMessage());
        }
    }


    private EvaluationResult mapResult(ResultSet rs) throws SQLException {
        EvaluationResult result = new EvaluationResult(rs.getString("student_id"));
        result.setUnzipSuccess(rs.getInt("unzip_success")   == 1);
        result.setCompileSuccess(rs.getInt("compile_success") == 1);
        result.setRunSuccess(rs.getInt("run_success")       == 1);
        result.setOutputMatch(rs.getInt("output_match")     == 1);
        result.setErrorLog(rs.getString("error_log"));
        return result;
    }
}