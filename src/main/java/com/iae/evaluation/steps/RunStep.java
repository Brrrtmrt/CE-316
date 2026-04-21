package com.iae.evaluation.steps;

import com.iae.domain.Configuration;
import com.iae.domain.StudentSubmission;
import com.iae.infrastructure.CommandExecutor;


public class RunStep extends AbstractEvaluationStep {

    private final CommandExecutor commandExecutor;
    private final Configuration configuration;
    private final String[] programArguments;

    public RunStep(CommandExecutor commandExecutor, Configuration configuration, String[] programArguments) {
        this.commandExecutor = commandExecutor;
        this.configuration = configuration;
        this.programArguments = programArguments;
    }

    @Override
    protected void validate(StudentSubmission submission) throws Exception {
        super.validate(submission);

        if (!submission.getExecutableFile().exists()) {
            throw new IllegalStateException("Executable file does not exist. Compilation may have failed.");
        }
    }

    @Override
    protected StepResult doExecute(StudentSubmission submission) throws Exception {
        String runCommand = configuration.getRunCommand()
                .replace("{executable}", submission.getExecutableFile().getAbsolutePath())
                .replace("{arguments}", String.join(" ", programArguments));

        String output = commandExecutor.executeAndCapture(runCommand, submission.getExtractedDir());

        submission.setProgramOutput(output);

        return StepResult.success(getStepName(),
                "Program executed successfully for student: " + submission.getStudentId());
    }

    @Override
    protected String getStepName() {
        return "RUN";
    }
}
