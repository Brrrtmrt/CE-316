package com.iae.stress;

import com.iae.infrastructure.CommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("stress")
public class MaliciousStudentStressTest {

    private CommandExecutor executor;
    private File tempDir;
    private String pythonCmd;

    @BeforeEach
    void setUp() throws IOException {
        executor = new CommandExecutor();
        tempDir = Files.createTempDirectory("iae_malicious_test").toFile();
        pythonCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
    }

    @Test
    @DisplayName("Infinite Loop Test: Ensure the 10-second timeout strictly kills the process")
    void testInfiniteLoopTimeout() throws IOException {
        File pyFile = new File(tempDir, "infinite.py");
        Files.writeString(pyFile.toPath(), "while True: pass");

        long start = System.currentTimeMillis();

        // This should throw an IOException indicating a timeout
        IOException ex = assertThrows(IOException.class, () -> {
            executor.executeAndCapture(pythonCmd + " infinite.py", tempDir);
        });

        long duration = System.currentTimeMillis() - start;

        assertTrue(ex.getMessage().contains("timed out"), "Exception must state it timed out.");

        // 10 second time limit
        assertTrue(duration >= 10000 && duration < 15000,
                "Process should have been killed right after the 10s timeout. Took: " + duration + "ms");
    }

    @Test
    @DisplayName("Stdout Flood: Ensure output is truncated to prevent OutOfMemoryError")
    void testMassiveOutputMemoryExhaustion() throws IOException, InterruptedException {
        File pyFile = new File(tempDir, "flood.py");
        // 50 million "A"s
        Files.writeString(pyFile.toPath(), "print('A' * 50_000_000)");

        CommandExecutor.ExecutionOutput output = executor.executeAndCapture(pythonCmd + " flood.py", tempDir);

        assertTrue(output.output().contains("[TRUNCATED]"), "Output must contain the truncation warning.");
        assertTrue(output.output().length() < 2_000_000, "Output length should be bounded to around 1MB.");
    }
}