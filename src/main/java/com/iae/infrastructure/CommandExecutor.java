package com.iae.infrastructure;

import java.io.*;
import java.util.concurrent.TimeUnit;


/**
 * CommandExecutor
 *
 * <p>This class provides methods to execute external commands (compilers, interpreters,
 * executables) and capture their output. It uses Java's ProcessBuilder for safe
 * command execution.</p>
 *
 * <h2>Functionality:</h2>
 * <ul>
 *   <li>Execute commands with working directory control</li>
 *   <li>Capture stdout and stderr output</li>
 *   <li>Timeout protection (prevents hanging)</li>
 *   <li>Proper exit code handling</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * CommandExecutor executor = new CommandExecutor();
 *
 * // Compile C program
 * int exitCode = executor.execute("gcc main.c -o main", new File("/tmp/student123"));
 * if (exitCode == 0) {
 *     System.out.println("Compilation successful");
 * }
 *
 * // Run program and capture output
 * String output = executor.executeAndCapture("./main arg1 arg2", new File("/tmp/student123"));
 * }</pre>
 *
 * <h2>MT-Unsafe:</h2>
 * <p>This class is <strong>NOT</strong> thread-safe. Each evaluation should have its own instance.</p>
 *
 * @author MS
 * @version 1.0
 * @since 0.0
 */
public class CommandExecutor {

    private static final String os = System.getProperty("os.name").toLowerCase();
    private static final String separator = System.lineSeparator();
    private static final long DEFAULT_TIMEOUT_SECONDS = 30; // TODO: DB config


    /**
     * Executes a command and returns its exit code.
     *
     * <p>This method is used for compilation where we only need to know
     * if the command succeeded (exit code 0) or failed (non-zero exit code).</p>
     *
     * @param command          the command to execute (e.g., "gcc main.c -o main")
     * @param workingDirectory the directory in which to execute the command
     * @return the exit code (0 = success, non-zero = failure)
     * @throws IOException          if command execution fails
     * @throws InterruptedException if execution is interrupted
     */
    public int execute(String command, File workingDirectory) throws IOException, InterruptedException {

        ProcessBuilder pb = createProcessBuilder(command, workingDirectory).redirectErrorStream(true); // Merge stderr into stdout
        Process process = pb.start();

        // Consume output to prevent deadlock
        Thread outputConsumer = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) ; // Ignore output
            } catch (IOException e) {
                // Ignore
            }
        });
        outputConsumer.start();

        boolean completed = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            outputConsumer.interrupt();
            throw new IOException("Command timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds: " + command);
        }

        outputConsumer.join();
        return process.exitValue();
    }

    /**
     * Executes a command and captures its output.
     *
     * <p>This method is used for running student programs where we need
     * to capture stdout to compare against expected output.</p>
     *
     * <p>Both stdout and stderr are captured and combined.</p>
     *
     * @param command          the command to execute (e.g., "./main arg1 arg2")
     * @param workingDirectory the directory in which to execute the command
     * @return the captured output (stdout + stderr)
     * @throws IOException          if command execution fails
     * @throws InterruptedException if execution is interrupted
     */
    public String executeAndCapture(String command, File workingDirectory) throws IOException, InterruptedException {

        ProcessBuilder pb = createProcessBuilder(command, workingDirectory).redirectErrorStream(true); // Merge stderr into stdout

        Process process = pb.start();

        // Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(separator);
            }
        }

        boolean completed = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Command timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds: " + command);
        }

        return output.toString();
    }

    /**
     * Creates a ProcessBuilder for the given command.
     * We only target Windows but good to keep for future use.
     * <p>Handles command parsing on Windows vs Unix systems. On Windows, commands
     * are executed through cmd.exe. On Unix, through sh.</p>
     *
     * @param command          the command string to parse
     * @param workingDirectory the working directory for execution
     * @return configured ProcessBuilder
     */
    private ProcessBuilder createProcessBuilder(String command, File workingDirectory) {

        ProcessBuilder pb;

        if (os.contains("win")) {
            // Windows: use cmd.exe
            pb = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            // Unix/Linux/Mac: use sh
            pb = new ProcessBuilder("sh", "-c", command);
        }

        pb.directory(workingDirectory);
        return pb;
    }
}
