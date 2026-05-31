package com.iae.evaluation.steps;

import com.iae.domain.Configuration;
import com.iae.domain.StudentSubmission;
import com.iae.infrastructure.CommandExecutor;

import java.util.logging.Logger;

public class CompileStep extends AbstractEvaluationStep {

    private static final Logger logger = Logger.getLogger(CompileStep.class.getName());

    private final CommandExecutor commandExecutor;
    private final Configuration configuration;

    public CompileStep(CommandExecutor commandExecutor, Configuration configuration) {
        this.commandExecutor = commandExecutor;
        this.configuration = configuration;
    }

    @Override
    protected void validate(StudentSubmission submission) throws Exception {
        super.validate(submission);

        if (submission.getSourceFile() == null) {
            throw new IllegalStateException("Source file not set on submission — UnzipStep may have failed");
        }

        if (!submission.getSourceFile().exists()) {
            throw new IllegalStateException("Source file does not exist: " + submission.getSourceFile());
        }

        if (submission.getExecutableFile() == null) {
            throw new IllegalStateException("Executable file path not set on submission — UnzipStep may have failed");
        }
    }

    @Override
    protected StepResult doExecute(StudentSubmission submission) throws Exception {
        // Skip compilation for interpreted languages (Python, JavaScript, etc.)
        if (configuration.getCompileCommand() == null || configuration.getCompileCommand().isBlank()) {
            logger.info("Skipping compilation for interpreted language: " + configuration.getLanguage());
            return StepResult.success(getStepName(),
                    "Compilation skipped for interpreted language");
        }

        String compileCommand = configuration.getCompileCommand()
                .replace("{src}", submission.getSourceFile() != null
                        ? submission.getSourceFile().getAbsolutePath() : "")
                .replace("{out}", submission.getExecutableFile().getAbsolutePath())
                .replace("{dir}", submission.getExtractedDir().getAbsolutePath());

        // Remove unresolved placeholders so they don't reach the shell
        compileCommand = compileCommand.replaceAll("\\{\\w+\\}", "").trim();

        logger.info("Compiling for student " + submission.getStudentId() + ": " + compileCommand);

        int exitCode = commandExecutor.execute(compileCommand, submission.getExtractedDir());

        if (exitCode == 0) {
            logger.info("Compilation successful for student: " + submission.getStudentId());
            return StepResult.success(getStepName(),
                    "Compilation successful for student: " + submission.getStudentId());
        } else {
            logger.warning("Compilation failed for student " + submission.getStudentId()
                    + " with exit code: " + exitCode);
            return StepResult.failure(getStepName(),
                    "Compilation failed with exit code: " + exitCode);
        }
    }

    @Override
    protected String getStepName() {
        return "COMPILE";
    }
}
