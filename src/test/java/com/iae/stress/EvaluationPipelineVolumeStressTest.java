package com.iae.stress;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.domain.EvaluationResult;
import com.iae.domain.Project;
import com.iae.domain.Status;
import com.iae.evaluation.EvaluationFacade;
import com.iae.evaluation.strategies.TrimLinesStrategy;
import com.iae.infrastructure.CommandExecutor;
import com.iae.infrastructure.FileSystemManager;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("stress")
public class EvaluationPipelineVolumeStressTest {

    private static final int SUBMISSION_COUNT = 500;
    private Path submissionsDir;
    private EvaluationFacade facade;
    private FileSystemManager fsm;

    @BeforeEach
    void setUp() throws IOException {
        submissionsDir = Files.createTempDirectory("iae_stress_submissions");
        fsm = new FileSystemManager();
        facade = new EvaluationFacade(new CommandExecutor(), fsm);
    }

    @AfterEach
    void tearDown() throws IOException {
        fsm.cleanupAll();
        Files.walk(submissionsDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> p.toFile().delete());
    }

    @Test
    @DisplayName("Evaluate 500 Python ZIP submissions in one go")
    @Timeout(300) // 5 minutes max
    void testMassiveSubmissionVolume() throws Exception {

        for (int i = 0; i < SUBMISSION_COUNT; i++) {
            createPythonZip("student_" + i, "print('Stress Test Pass')");
        }

        String pythonCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";

        Configuration pythonConfig = new ConfigurationBuilder()
                .setName("Python Stress Config")
                .setLanguage("Python")
                .setFileExtension("py")
                .setCompileCommand("")
                .setRunCommand(pythonCmd + " \"{src}\"")
                .setComparisonStrategy(new TrimLinesStrategy())
                .build();

        Project project = new Project(pythonConfig, submissionsDir.toString(), new String[0], "Stress Test Pass");

        long startTime = System.currentTimeMillis();
        List<EvaluationResult> results = facade.evaluateProject(project);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Processed " + SUBMISSION_COUNT + " submissions in " + duration + "ms.");

        if(!results.isEmpty() && results.get(0).getStatus() != Status.PASS) {
            System.err.println("First failure log: " + results.get(0).getErrorLog());
            System.err.println("First program output: " + results.get(0).getProgramOutput());
        }

        assertEquals(SUBMISSION_COUNT, results.size(), "Should process exactly " + SUBMISSION_COUNT + " submissions");

        long passCount = results.stream().filter(r -> r.getStatus() == Status.PASS).count();
        assertEquals(SUBMISSION_COUNT, passCount, "All submissions should pass.");
    }

    private void createPythonZip(String studentId, String pythonCode) throws IOException {
        File zipFile = new File(submissionsDir.toFile(), studentId + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry("main.py");
            zos.putNextEntry(entry);
            zos.write(pythonCode.getBytes());
            zos.closeEntry();
        }
    }
}