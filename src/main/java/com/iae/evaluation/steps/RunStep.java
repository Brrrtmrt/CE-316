package com.iae.evaluation.steps;

import com.iae.domain.Configuration;
import com.iae.domain.StudentSubmission;
import com.iae.infrastructure.CommandExecutor;

import java.util.logging.Logger;

public class RunStep extends AbstractEvaluationStep {

    private static final Logger logger = Logger.getLogger(RunStep.class.getName());

    private final CommandExecutor commandExecutor;
    private final Configuration configuration;
    private final String[] programArguments;

    public RunStep(CommandExecutor commandExecutor, Configuration configuration, String[] programArguments) {
        this.commandExecutor = commandExecutor;
        this.configuration = configuration;
        this.programArguments = programArguments != null ? programArguments : new String[0];
    }

    @Override
    protected void validate(StudentSubmission submission) throws Exception {
        super.validate(submission);

        if (submission.getExecutableFile() == null) {
            throw new IllegalStateException("Executable file path not set on submission");
        }

        // Only validate executable existence if the run command actually relies on the {out} variable
        if (configuration.getRunCommand().contains("{out}") && !submission.getExecutableFile().exists()) {
            throw new IllegalStateException("Executable does not exist — compilation may have failed: "
                    + submission.getExecutableFile());
        }
    }

    @Override
    protected StepResult doExecute(StudentSubmission submission) throws Exception {
        // Config template uses {out} for the executable path and {args} for program arguments.
        String srcPath = submission.getSourceFile() != null ? submission.getSourceFile().getAbsolutePath() : "";
        String args = String.join(" ", programArguments);
        String runCommand = configuration.getRunCommand()
                .replace("{out}", submission.getExecutableFile().getAbsolutePath())
                .replace("{src}", srcPath) // Source ...
                .replace("{args}", args)
                .replace("{dir}", submission.getExtractedDir().getAbsolutePath());

        // Trim any trailing whitespace left by an empty {args} substitution.
        runCommand = runCommand.trim();

        logger.info("Running for student " + submission.getStudentId() + ": " + runCommand);

        CommandExecutor.ExecutionOutput execOutput = commandExecutor.executeAndCapture(runCommand, submission.getExtractedDir());

        submission.setProgramOutput(execOutput.output());

        if (execOutput.exitCode() != 0) {
            logger.warning("Execution failed for student " + submission.getStudentId() + " with exit code: " + execOutput.exitCode());
            return StepResult.failure(getStepName(),
                    "Execution failed with exit code: " + execOutput.exitCode());
        }

        logger.info("Execution complete for student: " + submission.getStudentId());

        return StepResult.success(getStepName(),
                "Program executed successfully for student: " + submission.getStudentId());
    }

    @Override
    protected String getStepName() {
        return "RUN";
    }
}
