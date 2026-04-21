package com.iae.evaluation;

import com.iae.domain.Configuration;
import com.iae.domain.EvaluationResult;
import com.iae.domain.Project;
import com.iae.domain.StudentSubmission;
import com.iae.evaluation.steps.*;
import com.iae.evaluation.strategies.ComparisonStrategy;
import com.iae.infrastructure.CommandExecutor;
import com.iae.infrastructure.ZipHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EvaluationFacade {

    private final ZipHandler zipHandler;
    private final CommandExecutor commandExecutor;

    public EvaluationFacade(ZipHandler zipHandler, CommandExecutor commandExecutor) {
        this.zipHandler = zipHandler;
        this.commandExecutor = commandExecutor;
    }

    public List<EvaluationResult> evaluateProject(Project project) {
        List<EvaluationResult> results = new ArrayList<>();

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
                results.add(EvaluationResult.error(studentId, "Evaluation failed: " + e.getMessage()));
            }
        }

        return results;
    }

    private EvaluationResult evaluateSubmission(File zipFile, Configuration config,
                                                ComparisonStrategy strategy, Project project) {
        String studentId = extractStudentId(zipFile);
        StudentSubmission submission = new StudentSubmission(studentId, zipFile);

        EvaluationResult result = new EvaluationResult(studentId);


        UnzipStep unzipStep = new UnzipStep(zipHandler);
        StepResult unzipResult = unzipStep.execute(submission);
        result.setUnzipSuccess(unzipResult.isSuccess());

        if (!unzipResult.isSuccess()) {
            result.setErrorLog(unzipResult.getErrorDetails());
            return result;
        }

        CompileStep compileStep = new CompileStep(commandExecutor, config);
        StepResult compileResult = compileStep.execute(submission);
        result.setCompileSuccess(compileResult.isSuccess());

        if (!compileResult.isSuccess()) {
            result.setErrorLog(compileResult.getErrorDetails());
            return result;
        }

        RunStep runStep = new RunStep(commandExecutor, config, project.getProgramArguments());
        StepResult runResult = runStep.execute(submission);
        result.setRunSuccess(runResult.isSuccess());

        if (!runResult.isSuccess()) {
            result.setErrorLog(runResult.getErrorDetails());
            return result;
        }

        CompareStep compareStep = new CompareStep(strategy, project.getExpectedOutput());
        StepResult compareResult = compareStep.execute(submission);
        result.setOutputMatch(compareResult.isSuccess());

        if (!compareResult.isSuccess()) {
            result.setErrorLog("Output mismatch");
        }

        return result;
    }

    private String extractStudentId(File zipFile) {
        String filename = zipFile.getName();
        return filename.substring(0, filename.lastIndexOf('.'));
    }

    private ComparisonStrategy createComparisonStrategy(Configuration config) {
        return config.getComparisonStrategy();
    }
}
