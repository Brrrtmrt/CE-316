package com.iae.service;

import com.iae.domain.EvaluationResult;
import com.iae.domain.Project;
import com.iae.evaluation.EvaluationFacade;
import com.iae.infrastructure.CommandExecutor;
import com.iae.infrastructure.FileSystemManager;
import com.iae.persistence.dao.ResultDAO;

import java.util.List;

import static com.iae.domain.Status.*;

/**
 * EvaluationService
 *
 * <p>This service handles the evaluation of student submissions by coordinating
 * between the evaluation engine, infrastructure components, and data persistence layer.
 * It follows the Service Layer pattern to encapsulate business logic.</p>
 *
 * <h2>Functionality:</h2>
 * <ul>
 *   <li>Coordinate evaluation workflow through EvaluationFacade</li>
 *   <li>Manage infrastructure components (CommandExecutor, ZipHandler, FileSystemManager)</li>
 *   <li>Persist evaluation results to database</li>
 *   <li>Handle cleanup of temporary resources</li>
 *   <li>Provide evaluation status and progress tracking</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * EvaluationService service = new EvaluationService();
 *
 * // Evaluate a project
 * List<EvaluationResult> results = service.evaluateProject(project);
 *
 * // Results are automatically saved to database
 * for (EvaluationResult result : results) {
 *     System.out.println(result.getStudentId() + ": " + result.getStatus());
 * }
 * }</pre>
 *
 * <h2>Design Patterns:</h2>
 * <ul>
 *   <li>Service Layer - Encapsulates business logic</li>
 *   <li>Facade Pattern - Uses EvaluationFacade to simplify evaluation workflow</li>
 *   <li>Dependency Injection - Infrastructure components injected or created</li>
 * </ul>
 *
 * <h2>MT-Unsafe:</h2>
 * <p>This class is NOT thread-safe. Each evaluation should use its own instance.</p>
 *
 * @author IAE Development Team
 * @version 1.0
 * @since 0.0
 */
public class EvaluationService {

    private final EvaluationFacade evaluationFacade;
    private final ResultDAO resultDAO;
    private final FileSystemManager fileSystemManager;


    public EvaluationService() {
        this.fileSystemManager = new FileSystemManager();
        CommandExecutor commandExecutor = new CommandExecutor();

        this.evaluationFacade = new EvaluationFacade(commandExecutor, fileSystemManager);
        this.resultDAO = new ResultDAO();
    }

    /**
     * Constructs an EvaluationService with injected dependencies.
     * For testing with mock objects.
     */
    public EvaluationService(EvaluationFacade evaluationFacade, ResultDAO resultDAO,
                             FileSystemManager fileSystemManager) {
        this.evaluationFacade = evaluationFacade;
        this.resultDAO = resultDAO;
        this.fileSystemManager = fileSystemManager;
    }

    /**
     * Evaluates all student submissions for a project.
     * <p><strong>Note:</strong> Cleanup is performed in a finally block to ensure
     * temporary files are removed even if evaluation fails.</p>
     *
     * @param project the project containing submissions to evaluate
     * @return list of evaluation results for all students
     * @throws IllegalArgumentException if project is null or invalid
     * @throws EvaluationException      if evaluation fails
     */
    public List<EvaluationResult> evaluateProject(Project project) {

        validateProject(project);

        try {
            List<EvaluationResult> results = evaluationFacade.evaluateProject(project);

            saveResults(project, results);

            project.setLastRunDate(java.time.LocalDateTime.now());

            return results;

        } catch (Exception e) {
            throw new EvaluationException("Failed to evaluate project: " + project.getName(), e);
        } finally {
            cleanup();
        }
    }

    /**
     * Retrieves evaluation results for a specific project.
     *
     * @param projectId the project ID
     * @return list of evaluation results for the project
     * @throws IllegalArgumentException if projectId is null or empty
     */
    public List<EvaluationResult> getResultsForProject(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }

        return resultDAO.findByProjectId(projectId);
    }

    /**
     * Retrieves evaluation result for a specific student in a project.
     *
     * @param projectId the project ID
     * @param studentId the student ID
     * @return the evaluation result, or null if not found
     * @throws IllegalArgumentException if projectId or studentId is null or empty
     */
    public EvaluationResult getResultForStudent(String projectId, String studentId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }

        return resultDAO.findByProjectAndStudent(projectId, studentId);
    }

    /**
     * Deletes all evaluation results for a project.
     * Useful when re-evaluating a project.
     *
     * @param projectId the project ID
     * @throws IllegalArgumentException if projectId is null or empty
     */
    public void deleteResultsForProject(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }

        resultDAO.deleteByProjectId(projectId);
    }

    /**
     * Gets statistics for a project's evaluation results.
     *
     * @param projectId the project ID
     * @return evaluation statistics (pass count, fail count, error count)
     */
    public EvaluationStatistics getStatistics(String projectId) {
        List<EvaluationResult> results = getResultsForProject(projectId);
        return EvaluationStatistics.from(results);
    }

    /**
     * Validates that a project has all required information for evaluation.
     *
     * @param project the project to validate
     * @throws IllegalArgumentException if project is invalid
     */
    private void validateProject(Project project) {

        if (project == null) throw new IllegalArgumentException("Project cannot be null");

        if (project.getConfiguration() == null) {
            throw new IllegalArgumentException("Project must have a configuration");
        }

        if (project.getSubmissionsDirectory() == null || project.getSubmissionsDirectory().trim().isEmpty()) {
            throw new IllegalArgumentException("Project must have a submissions directory");
        }

        java.io.File submissionsDir = new java.io.File(project.getSubmissionsDirectory());
        if (!submissionsDir.exists() || !submissionsDir.isDirectory()) {
            throw new IllegalArgumentException("Submissions directory does not exist: " +
                    project.getSubmissionsDirectory());
        }
    }

    /**
     * Saves evaluation results to database.
     *
     * @param project the project being evaluated
     * @param results the evaluation results to save
     */
    private void saveResults(Project project, List<EvaluationResult> results) {
        for (EvaluationResult result : results) {
            resultDAO.save(project.getId(), result);
        }
    }

    private void cleanup() {
        try {
            fileSystemManager.cleanupAll();
        } catch (Exception e) {
            System.err.println("Warning: Failed to cleanup temporary files: " + e.getMessage());
        }
    }

    public static class EvaluationException extends RuntimeException {
        public EvaluationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class EvaluationStatistics {
        private final int totalCount;
        private final int passCount;
        private final int failCount;
        private final int errorCount;

        private EvaluationStatistics(int totalCount, int passCount, int failCount, int errorCount) {
            this.totalCount = totalCount;
            this.passCount = passCount;
            this.failCount = failCount;
            this.errorCount = errorCount;
        }

        public static EvaluationStatistics from(List<EvaluationResult> results) {
            int pass = 0, fail = 0, error = 0;

            for (EvaluationResult result : results) {
                switch (result.getStatus()) {
                    case PASS:
                        pass++;
                        break;
                    case FAIL:
                        fail++;
                        break;
                    case ERROR:
                        error++;
                        break;
                }
            }

            return new EvaluationStatistics(results.size(), pass, fail, error);
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getPassCount() {
            return passCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public double getPassPercentage() {
            return totalCount == 0 ? 0.0 : (passCount * 100.0) / totalCount;
        }

        @Override
        public String toString() {
            return String.format("Total: %d, Pass: %d (%.1f%%), Fail: %d, Error: %d",
                    totalCount, passCount, getPassPercentage(), failCount, errorCount);
        }
    }
}
