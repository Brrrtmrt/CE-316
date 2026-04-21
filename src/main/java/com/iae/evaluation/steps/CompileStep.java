package com.iae.evaluation.steps;

import com.iae.domain.Configuration;
import com.iae.domain.StudentSubmission;
import com.iae.infrastructure.CommandExecutor;

public class CompileStep extends AbstractEvaluationStep {

    private final CommandExecutor commandExecutor;
    private final Configuration configuration;

    public CompileStep(CommandExecutor commandExecutor, Configuration configuration) {
        this.commandExecutor = commandExecutor;
        this.configuration = configuration;
    }

    @Override
    protected void validate(StudentSubmission submission) throws Exception {
        super.validate(submission);

        if (configuration.getCompileCommand() == null) {
            throw new IllegalStateException("Compile command not configured");
        }
    }

    @Override
    protected StepResult doExecute(StudentSubmission submission) throws Exception {
        String compileCommand = configuration.getCompileCommand()
                .replace("{sourceFile}", submission.getSourceFile().getAbsolutePath())
                .replace("{outputFile}", submission.getExecutableFile().getAbsolutePath());

        int exitCode = commandExecutor.execute(compileCommand, submission.getExtractedDir());

        if (exitCode == 0) {
            return StepResult.success(getStepName(),
                    "Compilation successful for student: " + submission.getStudentId());
        } else {
            return StepResult.failure(getStepName(),
                    "Compilation failed with exit code: " + exitCode);
        }
    }

    @Override
    protected String getStepName() {
        return "COMPILE";
    }
}
