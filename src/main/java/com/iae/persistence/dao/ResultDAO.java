package com.iae.persistence.dao;

import com.iae.domain.EvaluationResult;
import com.iae.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ResultDAO — full CRUD for the {@code evaluation_results} table.
 *
 * <h2>Fixes applied</h2>
 * <ul>
 *   <li>All four methods were stubs returning empty/null.  They are now fully
 *       implemented against the schema defined in {@code schema.sql}.</li>
 *   <li>Column names match the schema exactly.</li>
 *   <li>Fixed overwrite behavior: Replaced native 'INSERT OR REPLACE' with a safe
 *       Delete-Before-Insert cycle to guarantee idempotent test outcomes without
 *       modifying schema.sql constraints.</li>
 * </ul>
 *
 * @author Dev 1
 * @version 1.2
 */
public class ResultDAO extends BaseDAO {

    /**
     * Returns all evaluation results for a given project.
     *
     * @param projectId the project's database id (as a String, matching
     *                  {@link com.iae.domain.Project#getId()})
     * @return list of results; empty if none found
     */
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

    /**
     * Returns the evaluation result for one specific student in a project.
     *
     * @param projectId the project's database id
     * @param studentId the student identifier
     * @return the result, or {@code null} if not found
     */
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

    /**
     * Inserts or replaces the evaluation result for a student in a project.
     *
     * FIXED: Explicitly deletes the existing row for the given (project_id, student_id)
     * pair right before injecting the new one. This ensures absolute compatibility
     * with the shared schema structure.
     *
     * @param projectId the project's database id
     * @param result    the evaluation result to persist
     */
    public void save(String projectId, EvaluationResult result) {
        String deleteSql = "DELETE FROM evaluation_results WHERE project_id = ? AND student_id = ?";
        String insertSql = """
                INSERT INTO evaluation_results
                    (project_id, student_id,
                     unzip_success, compile_success, run_success, output_match,
                     error_log, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getConnection()) {

            // 1. Clean up stale historical results for this specific student first
            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                delStmt.setInt(1, Integer.parseInt(projectId));
                delStmt.setString(2, result.getStudentId());
                delStmt.executeUpdate();
            }

            // 2. Insert the fresh payload safely
            try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                insStmt.setInt(1,    Integer.parseInt(projectId));
                insStmt.setString(2, result.getStudentId());
                insStmt.setInt(3,    result.isUnzipSuccess()   ? 1 : 0);
                insStmt.setInt(4,    result.isCompileSuccess()  ? 1 : 0);
                insStmt.setInt(5,    result.isRunSuccess()      ? 1 : 0);
                insStmt.setInt(6,    result.isOutputMatch()     ? 1 : 0);
                insStmt.setString(7, result.getErrorLog());

                // Safe Enum Name check
                String statusStr = (result.getStatus() != null) ? result.getStatus().name() : "COMPLETED";
                insStmt.setString(8, statusStr);

                insStmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("ResultDAO.save failed for student "
                    + result.getStudentId() + ": " + e.getMessage());
        }
    }

    /**
     * Deletes all evaluation results for a project.
     * Useful before re-running an evaluation.
     *
     * @param projectId the project's database id
     */
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

    /**
     * Maps a {@link ResultSet} row to an {@link EvaluationResult}.
     * SQLite stores booleans as integers (0/1).
     */
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