package com.iae.evaluation.steps;

import java.util.logging.Logger;

import com.iae.domain.Configuration;
import com.iae.domain.StudentSubmission;
import com.iae.infrastructure.CommandExecutor;

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

        // Sadece çalıştırılacak komutta {out} değişkeni kullanılıyorsa (ör. C/C++ için) .exe dosyasının varlığını kontrol et.
        // Java gibi diller (.class üreten) {out} kullanmadığı için bu kontrolü atlamalıdır.
        if (configuration.getRunCommand() != null && configuration.getRunCommand().contains("{out}")) {
            if (!submission.getExecutableFile().exists()) {
                throw new IllegalStateException("Executable does not exist — compilation may have failed: "
                        + submission.getExecutableFile());
            }
        }
    }

    @Override
    protected StepResult doExecute(StudentSubmission submission) throws Exception {
        // Config template uses {out} for the executable path and {args} for program arguments.
        String args = String.join(" ", programArguments);
        String runCommand = configuration.getRunCommand()
                .replace("{out}", submission.getExecutableFile().getAbsolutePath())
                .replace("{args}", args);

        // Trim any trailing whitespace left by an empty {args} substitution.
        runCommand = runCommand.trim();

        logger.info("Running for student " + submission.getStudentId() + ": " + runCommand);

        String output = commandExecutor.executeAndCapture(runCommand, submission.getExtractedDir());

        submission.setProgramOutput(output);

        logger.info("Execution complete for student: " + submission.getStudentId());

        return StepResult.success(getStepName(),
                "Program executed successfully for student: " + submission.getStudentId());
    }

    @Override
    protected String getStepName() {
        return "RUN";
    }
}
