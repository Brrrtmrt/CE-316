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

class CompileStepTest {

    @TempDir
    Path tempDir;

    private static class StubExecutor extends CommandExecutor {
        String lastCommand;
        final int exitCode;

        StubExecutor(int exitCode) {
            this.exitCode = exitCode;
        }

        @Override
        public int execute(String command, File dir) throws IOException, InterruptedException {
            lastCommand = command;
            return exitCode;
        }
    }

    private Configuration config(String compileCmd) {
        return new Configuration("Test", "Java", ".java", compileCmd, "java Main", new ExactMatchStrategy(), "test");
    }

    private StudentSubmission submissionWithSourceFile() throws IOException {
        File source = tempDir.resolve("Main.java").toFile();
        source.createNewFile();
        StudentSubmission sub = new StudentSubmission("s1", tempDir.resolve("student.zip").toFile());
        sub.setSourceFile(source);
        sub.setExecutableFile(tempDir.resolve("Main.exe").toFile());
        return sub;
    }

    @Test
    void nullSubmission_returnsError() {
        StepResult result = new CompileStep(new CommandExecutor(), config("javac {src}")).execute(null);
        assertFalse(result.isSuccess());
        assertEquals("COMPILE", result.getStepName());
    }

    @Test
    void sourceFileNotSet_returnsError() {
        StudentSubmission sub = new StudentSubmission("s1", tempDir.resolve("student.zip").toFile());
        sub.setExecutableFile(tempDir.resolve("Main.exe").toFile());
        assertFalse(new CompileStep(new CommandExecutor(), config("javac {src}")).execute(sub).isSuccess());
    }

    @Test
    void blankCompileCommand_skipsCompilationAndSucceeds() throws IOException {
        StepResult result = new CompileStep(new CommandExecutor(), config("")).execute(submissionWithSourceFile());
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("skip"));
    }

    @Test
    void exitCode0_returnsSuccess() throws IOException {
        StepResult result = new CompileStep(new StubExecutor(0), config("javac {src}"))
                .execute(submissionWithSourceFile());
        assertTrue(result.isSuccess());
        assertEquals("COMPILE", result.getStepName());
    }

    @Test
    void nonZeroExitCode_returnsFailure() throws IOException {
        StepResult result = new CompileStep(new StubExecutor(1), config("javac {src}"))
                .execute(submissionWithSourceFile());
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("1"));
    }

    @Test
    void placeholders_substitutedWithAbsolutePaths() throws IOException {
        StubExecutor exec = new StubExecutor(0);
        StudentSubmission sub = submissionWithSourceFile();
        new CompileStep(exec, config("javac {src} -o {out}")).execute(sub);
        assertFalse(exec.lastCommand.contains("{src}"));
        assertFalse(exec.lastCommand.contains("{out}"));
        assertTrue(exec.lastCommand.contains(sub.getSourceFile().getAbsolutePath()));
        assertTrue(exec.lastCommand.contains(sub.getExecutableFile().getAbsolutePath()));
    }
}
