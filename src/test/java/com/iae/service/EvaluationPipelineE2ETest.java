package com.iae.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.domain.EvaluationResult;
import com.iae.domain.Project;
import com.iae.domain.Status;
import com.iae.evaluation.EvaluationFacade;
import com.iae.evaluation.strategies.TrimLinesStrategy;
import com.iae.infrastructure.CommandExecutor;
import com.iae.infrastructure.FileSystemManager;

public class EvaluationPipelineE2ETest {

    private EvaluationFacade evaluationFacade;
    private FileSystemManager fileSystemManager;
    
    @TempDir
    Path sharedTempDir;

    private Path submissionsDir;

    @BeforeEach
    public void setUp() throws IOException {
        fileSystemManager = new FileSystemManager();
        CommandExecutor commandExecutor = new CommandExecutor();
        evaluationFacade = new EvaluationFacade(commandExecutor, fileSystemManager);

        // The actual pipeline requires submissions to be zipped in a dedicated directory
        submissionsDir = sharedTempDir.resolve("submissions");
        Files.createDirectories(submissionsDir);
    }

    @AfterEach
    public void tearDown() {
        // Ensure that FSM temporary directories are cleared out after each test
        if (fileSystemManager != null) {
            fileSystemManager.cleanupAll();
        }
    }

    @Test
    public void testSuccessfulJavaCompilationAndExactMatchExecution() throws IOException {
        String javacCmd = getJavaExecutable("javac");
        String javaCmd = getJavaExecutable("java");

        // 1. Arrange: Setup configuration and project constraints
        Configuration javaConfig = new ConfigurationBuilder()
                .setName("Java Config")
                .setLanguage("Java")
                .setFileExtension("java")
                .setCompileCommand(javacCmd + " \"{src}\"")
                .setRunCommand(javaCmd + " -cp \"{dir}\" Main {args}")
                .setComparisonStrategy(new TrimLinesStrategy())
                .build();

        Project mockProject = new Project(javaConfig, submissionsDir.toString(), new String[]{"5", "10"}, "SUM: 15\n");
        mockProject.setName("CS101 - Addition Assignment");

        // Simulate a correct student submission file structure via a zipped artifact
        String validSourceCode = 
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        int a = Integer.parseInt(args[0]);\n" +
            "        int b = Integer.parseInt(args[1]);\n" +
            "        System.out.print(\"SUM: \" + (a + b) + \"\\n\");\n" +
            "    }\n" +
            "}";
        createStudentZip("20230602020", "Main.java", validSourceCode);

        // 2. Act: Execute the end-to-end evaluation transaction
        List<EvaluationResult> results = evaluationFacade.evaluateProject(mockProject);

        // 3. Assert: Verify each state transitions flawlessly to success
        assertEquals(1, results.size(), "Should evaluate exactly one submission.");
        EvaluationResult result = results.get(0);
        
        assertNotNull(result, "Evaluation result should not be null.");
        assertEquals("20230602020", result.getStudentId());
        assertTrue(result.isUnzipSuccess(), "Unzip step should succeed.");
        assertTrue(result.isCompileSuccess(), "Compile step should succeed.");
        assertTrue(result.isRunSuccess(), "Run step should succeed.");
        assertTrue(result.isOutputMatch(), "Output match step should succeed.");
        assertEquals(Status.PASS, result.getStatus(), "The submission should pass matching the expected output.");
        assertTrue(result.getErrorLog() == null || result.getErrorLog().isEmpty(), "Error logs should be empty.");
    }

    @Test
    public void testCompilationFailureScenario() throws IOException {
        String javacCmd = getJavaExecutable("javac");
        String javaCmd = getJavaExecutable("java");

        // 1. Arrange: Create invalid source code syntax
        Configuration javaConfig = new ConfigurationBuilder()
                .setName("Java Config")
                .setLanguage("Java")
                .setFileExtension("java")
                .setCompileCommand(javacCmd + " \"{src}\"")
                .setRunCommand(javaCmd + " -cp \"{dir}\" Main {args}")
                .setComparisonStrategy(new TrimLinesStrategy())
                .build();

        Project mockProject = new Project(javaConfig, submissionsDir.toString(), new String[0], "Expected output");
        mockProject.setName("Broken Project");

        String brokenSourceCode = "public class Main { broken_syntax_here }";
        createStudentZip("20210602013", "Main.java", brokenSourceCode);

        // 2. Act: Run compilation pipeline
        List<EvaluationResult> results = evaluationFacade.evaluateProject(mockProject);

        // 3. Assert: Pipeline halts safely and logs errors correctly
        assertEquals(1, results.size());
        EvaluationResult result = results.get(0);

        assertNotNull(result);
        assertEquals("20210602013", result.getStudentId());
        assertTrue(result.isUnzipSuccess(), "Unzip step should succeed.");
        assertFalse(result.isCompileSuccess(), "Compile step should fail.");
        assertEquals(Status.ERROR, result.getStatus(), "Should report a pipeline error (due to compile failure).");
        assertNotNull(result.getErrorLog(), "Compilation error logs must be populated.");
    }

    private void createStudentZip(String studentId, String fileName, String content) throws IOException {
        File zipFile = new File(submissionsDir.toFile(), studentId + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes());
            zos.closeEntry();
        }
    }

    private String getJavaExecutable(String command) {
        String javaHome = System.getProperty("java.home");
        File executable = new File(javaHome + File.separator + "bin", 
            command + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : ""));
        String path = executable.getAbsolutePath();
        // Avoid cmd.exe quote stripping bugs by only quoting if necessary
        return path.contains(" ") ? "\"" + path + "\"" : path;
    }
}