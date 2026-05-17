package com.iae.evaluation;

import com.iae.domain.Configuration;
import com.iae.domain.EvaluationResult;
import com.iae.domain.Project;
import com.iae.domain.StudentSubmission;
import com.iae.evaluation.steps.*;
import com.iae.evaluation.strategies.ComparisonStrategy;
import com.iae.infrastructure.CommandExecutor;
import com.iae.infrastructure.FileSystemManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.iae.infrastructure.FileSystemManager.sanitizeStudentID;

/**
 * EvaluationFacade
 *
 * <p>This class implements the Facade Pattern to provide a simple interface for
 * evaluating student submissions. It runs all evaluation steps (unzip, compile, run, compare).</p>
 *
 * <h2>Evaluation Process:</h2>
 * <ol>
 *   <li><strong>Unzip:</strong> Extract student ZIP file to temporary directory</li>
 *   <li><strong>Compile:</strong> Compile source code using configuration</li>
 *   <li><strong>Run:</strong> Execute program with provided arguments</li>
 *   <li><strong>Compare:</strong> Compare output with expected result</li>
 * </ol>
 *
 * <h2>Error Handling:</h2>
 * <p>If any step fails for a student, the error is logged and evaluation continues
 * with the next student. This ensures one student's failure doesn't stop the entire
 * evaluation process.</p>
 *
 * <h2>Design Patterns Used:</h2>
 * <ul>
 *   <li><strong>Facade Pattern:</strong> Simplifies complex evaluation workflow</li>
 *   <li><strong>Template Method Pattern:</strong> All steps extend AbstractEvaluationStep</li>
 *   <li><strong>Strategy Pattern:</strong> ComparisonStrategy for output comparison</li>
 * </ul>
 *
 * <h2>MT-Unsafe</h2>
 * <p>This class is <strong>NOT</strong> thread-safe. Each evaluation should use its own instance.</p>
 *
 * @author IAE Development Team
 * @version 1.0
 * @see com.iae.evaluation.steps.AbstractEvaluationStep
 * @see com.iae.evaluation.strategies.ComparisonStrategy
 * @since 0.0
 */
public class EvaluationFacade {
    private final CommandExecutor commandExecutor;

    private final FileSystemManager fileSystemManager;

    public EvaluationFacade(CommandExecutor commandExecutor, FileSystemManager fsm) {
        this.fileSystemManager = fsm;
        this.commandExecutor = commandExecutor;
    }

    /**
     * Evaluates all student submissions for a project.
     *
     * <p>It performs the following:</p>
     * <ol>
     *   <li>Retrieves project configuration and submissions directory</li>
     *   <li>Finds all ZIP files in submissions directory</li>
     *   <li>For each ZIP file:
     *     <ul>
     *       <li>Extracts to temporary directory</li>
     *       <li>Compiles source code</li>
     *       <li>Runs executable with arguments</li>
     *       <li>Compares output with expected result</li>
     *     </ul>
     *   </li>
     *   <li>Collects all results (including errors)</li>
     *   <li>Cleans up temporary files</li>
     * </ol>
     *
     * <p><strong>Error Handling:</strong> If evaluation fails for a student,
     * an error result is created and evaluation continues with the next student.
     * This ensures one failure doesn't stop the entire cluster.</p>
     *
     * <p><strong>Cleanup:</strong> Temporary files are cleaned up in a finally block
     * to ensure cleanup happens even if evaluation fails.</p>
     *
     * @param project the project containing configuration and submissions
     * @return list of evaluation results for all students (never null, might be empty)
     * @throws IllegalArgumentException if project is null
     */
    public List<EvaluationResult> evaluateProject(Project project) {

        List<EvaluationResult> results = new ArrayList<>();

        try {
            Configuration config = project.getConfiguration();
            File submissionsDir = new File(project.getSubmissionsDirectory());

            File[] zipFiles = submissionsDir.listFiles((dir, name) -> name.endsWith(".zip"));

            if (zipFiles == null || zipFiles.length == 0) {
                return results;
            }

            ComparisonStrategy comparisonStrategy = createComparisonStrategy(config);

            for (File zipFile : zipFiles) {
                try {
                    EvaluationResult result = evaluateSubmission(zipFile, config, comparisonStrategy, project);
                    results.add(result);
                } catch (Exception e) {
                    String studentId = extractStudentId(zipFile);
                    EvaluationResult err = EvaluationResult.error(studentId, "Evaluation failed: " + e.getMessage());
                    if (err != null) results.add(err);
                }
            }
        } finally {
            fileSystemManager.cleanupAll();
        }


        return results;
    }

    /**
     * Evaluates a single student submission through all evaluation steps.
     *
     * <p>Executes the steps:</p>
     * <ol>
     *   <li><strong>Unzip Step:</strong> Extract ZIP file</li>
     *   <li><strong>Compile Step:</strong> Compile source code</li>
     *   <li><strong>Run Step:</strong> Execute program</li>
     *   <li><strong>Compare Step:</strong> Compare output</li>
     * </ol>
     *
     * <p>If any step fails, the error is logged and subsequent steps are skipped.</p>
     *
     * @param zipFile  the student's ZIP file
     * @param config   the project configuration
     * @param strategy the comparison strategy to use
     * @param project  the project (for arguments and expected output)
     * @return evaluation result for this student
     */
    private EvaluationResult evaluateSubmission(File zipFile, Configuration config,
                                                ComparisonStrategy strategy, Project project) {
        String studentId = extractStudentId(zipFile);
        StudentSubmission submission = new StudentSubmission(studentId, zipFile);

        EvaluationResult result = submission.getEvaluationResult(); // Link directly to the submission's built-in result

        UnzipStep unzipStep = new UnzipStep(config);
        StepResult unzipResult = unzipStep.execute(submission);
        result.setUnzipSuccess(unzipResult.isSuccess());

        if (!unzipResult.isSuccess()) {
            result.setErrorLog(unzipResult.getErrorDetails());
            submission.setSubmissionStatus(result.getStatus());
            return result;
        }

        CompileStep compileStep = new CompileStep(commandExecutor, config);
        StepResult compileResult = compileStep.execute(submission);
        result.setCompileSuccess(compileResult.isSuccess());

        if (!compileResult.isSuccess()) {
            result.setErrorLog(compileResult.getErrorDetails());
            submission.setSubmissionStatus(result.getStatus());
            return result;
        }

        RunStep runStep = new RunStep(commandExecutor, config, project.getProgramArguments());
        StepResult runResult = runStep.execute(submission);
        result.setRunSuccess(runResult.isSuccess());

        // Capture the output generated during the run step
        result.setProgramOutput(submission.getProgramOutput());

        if (!runResult.isSuccess()) {
            result.setErrorLog(runResult.getErrorDetails());
            submission.setSubmissionStatus(result.getStatus());
            return result;
        }

        CompareStep compareStep = new CompareStep(strategy, project.getExpectedOutput());
        StepResult compareResult = compareStep.execute(submission);
        result.setOutputMatch(compareResult.isSuccess());

        if (!compareResult.isSuccess()) {
            result.setErrorLog("Output mismatch");
        }

        submission.setSubmissionStatus(result.getStatus()); // Synchronize the overall status

        return result;
    }

    /**
     * Extracts student ID from ZIP filename.
     *
     * <p>Removes the .zip extension from the filename to get the student ID.</p>
     *
     * @param zipFile the ZIP file
     * @return the student ID (without extension)
     */
    private String extractStudentId(File zipFile) {
        String filename = zipFile.getName();
        int dot = filename.lastIndexOf('.');
        String tmp = dot > 0 ? filename.substring(0, dot) : filename;
        return sanitizeStudentID(tmp);
    }

    /**
     * Creates the appropriate comparison strategy from configuration.
     *
     * @param config the project configuration
     * @return the comparison strategy to use
     * @throws IllegalStateException if ComparisonStrategy is null
     */
    private ComparisonStrategy createComparisonStrategy(Configuration config) {
        ComparisonStrategy strat = config.getComparisonStrategy();
        if (strat == null) {
            throw new IllegalStateException("Configuration must provide a comparison strategy");
        }
        return strat;
    }
}
