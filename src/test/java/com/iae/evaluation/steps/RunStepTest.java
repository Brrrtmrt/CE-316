package com.iae.evaluation.steps;

import com.iae.domain.Configuration;
import com.iae.domain.StudentSubmission;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.infrastructure.CommandExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RunStepTest {

    @TempDir
    Path tempDir;

    private static class StubExecutor extends CommandExecutor {
        String lastCommand;
        final int exitCode;
        final String output;

        StubExecutor(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        @Override
        public ExecutionOutput executeAndCapture(String command, File dir) throws IOException, InterruptedException {
            lastCommand = command;
            return new ExecutionOutput(exitCode, output);
        }
    }

    private Configuration config(String runCmd) {
        return new Configuration("Test", "Java", ".java", "", runCmd, new ExactMatchStrategy(), "test");
    }

    private StudentSubmission submissionWithExistingExecutable() throws IOException {
        File exe = tempDir.resolve("Main.exe").toFile();
        exe.createNewFile();
        StudentSubmission sub = new StudentSubmission("s1", tempDir.resolve("student.zip").toFile());
        sub.setExecutableFile(exe);
        return sub;
    }

    @Test
    void nullSubmission_returnsError() {
        StepResult result = new RunStep(new CommandExecutor(), config("{out}"), new String[0]).execute(null);
        assertFalse(result.isSuccess());
        assertEquals("RUN", result.getStepName());
    }

    @Test
    void executableNotExist_commandContainsOut_returnsError() {
        StudentSubmission sub = new StudentSubmission("s1", tempDir.resolve("student.zip").toFile());
        sub.setExecutableFile(tempDir.resolve("Main.exe").toFile()); // not created on disk
        assertFalse(new RunStep(new CommandExecutor(), config("{out}"), new String[0]).execute(sub).isSuccess());
    }

    @Test
    void successfulExecution_returnsSuccess() throws IOException {
        StepResult result = new RunStep(new StubExecutor(0, "Hello\n"), config("{out}"), new String[0])
                .execute(submissionWithExistingExecutable());
        assertTrue(result.isSuccess());
        assertEquals("RUN", result.getStepName());
    }

    @Test
    void successfulExecution_capturesProgramOutput() throws IOException {
        StudentSubmission sub = submissionWithExistingExecutable();
        new RunStep(new StubExecutor(0, "Hello World\n"), config("{out}"), new String[0]).execute(sub);
        assertEquals("Hello World\n", sub.getProgramOutput());
    }

    @Test
    void nonZeroExitCode_returnsFailure() throws IOException {
        StepResult result = new RunStep(new StubExecutor(1, ""), config("{out}"), new String[0])
                .execute(submissionWithExistingExecutable());
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("1"));
    }

    @Test
    void programArguments_joinedAndSubstituted() throws IOException {
        StubExecutor exec = new StubExecutor(0, "");
        new RunStep(exec, config("{out} {args}"), new String[]{"Alice", "42"})
                .execute(submissionWithExistingExecutable());
        assertTrue(exec.lastCommand.contains("Alice 42"));
    }

    @Test
    void emptyArgs_trailingWhitespaceIsTrimmed() throws IOException {
        StubExecutor exec = new StubExecutor(0, "");
        new RunStep(exec, config("{out} {args}"), new String[0]).execute(submissionWithExistingExecutable());
        assertFalse(exec.lastCommand.endsWith(" "));
    }
}
