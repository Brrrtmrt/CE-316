package com.iae.stress;

import com.iae.domain.EvaluationResult;
import com.iae.domain.Status;
import com.iae.persistence.DatabaseManager;
import com.iae.persistence.dao.ProjectDAO;
import com.iae.persistence.dao.ResultDAO;
import com.iae.domain.Project;
import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("stress")
public class DatabaseConcurrencyStressTest {

    private File tempDbFile;
    private ProjectDAO projectDAO;
    private ResultDAO resultDAO;
    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("iae_db_stress_", ".db");
        DatabaseManager.setDbUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath().replace("\\", "/"));
        DatabaseManager.initializeDatabase();

        projectDAO = new ProjectDAO();
        resultDAO = new ResultDAO();

        Configuration config = new ConfigurationBuilder()
                .setName("Dummy")
                .setLanguage("Java")
                .setFileExtension("")
                .setRunCommand("dummy")
                .setComparisonStrategy(new ExactMatchStrategy())
                .build();
        Project p = new Project(config, ".", new String[0], "");
        p.setName("Stress DB Project");
        projectDAO.save(p);
        projectId = p.getId();
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.resetDbUrl();
        tempDbFile.delete();
    }

    @Test
    @DisplayName("Save 5,000 results concurrently using 10 threads")
    void testConcurrentDatabaseWrites() throws Exception {
        int threads = 10;
        int insertsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            tasks.add(() -> {
                for (int i = 0; i < insertsPerThread; i++) {
                    EvaluationResult result = new EvaluationResult("student_" + threadId + "_" + i);

                    result.setUnzipSuccess(true);
                    result.setCompileSuccess(true);
                    result.setRunSuccess(true);

                    result.setOutputMatch(i % 2 == 0);
                    result.setProgramOutput("Output " + i);

                    resultDAO.save(projectId, result);
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get(); // Will throw an exception if the thread failed
        }
        executor.shutdown();

        List<EvaluationResult> dbResults = resultDAO.findByProjectId(projectId);
        assertEquals(threads * insertsPerThread, dbResults.size(), "Database lost rows under concurrent write load!");
    }
}