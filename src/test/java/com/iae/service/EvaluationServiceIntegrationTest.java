package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.domain.EvaluationResult;
import com.iae.domain.Project;
import com.iae.domain.Status;
import com.iae.evaluation.strategies.TrimLinesStrategy;
import com.iae.persistence.DatabaseManager;
import com.iae.persistence.dao.ProjectDAO;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EvaluationServiceIntegrationTest {

    private EvaluationService evaluationService;
    private Path tempSubmissionsDir;
    private File tempDbFile;
    private ProjectDAO projectDAO;

    @BeforeEach
    void setUp() throws Exception {
        tempSubmissionsDir = Files.createTempDirectory("iae_eval_submissions_");
        
        tempDbFile = File.createTempFile("iae_eval_test_", ".db");
        tempDbFile.deleteOnExit();

        DatabaseManager.setDbUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath().replace("\\", "/"));
        DatabaseManager.initializeDatabase();

        evaluationService = new EvaluationService();
        projectDAO = new ProjectDAO();
    }

    @AfterEach
    void tearDown() throws IOException {
        DatabaseManager.resetDbUrl();
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
        clearDirectory(tempSubmissionsDir);
    }

    @Test
    @DisplayName("Evaluate Java Submissions")
    void testJavaEvaluation() throws Exception {
        Assumptions.assumeTrue(isCommandAvailable("javac"), "javac not found in PATH");
        Assumptions.assumeTrue(isCommandAvailable("java"), "java not found in PATH");

        Configuration config = new ConfigurationBuilder()
                .setName("Java Config")
                .setLanguage("Java")
                .setFileExtension("java")
                .setCompileCommand("javac {src}")
                .setRunCommand("java -cp {dir} Main {args}")
                .setComparisonStrategy(new TrimLinesStrategy())
                .build();

        Project project = new Project(config, tempSubmissionsDir.toString(), new String[]{"World"}, "Hello World");
        project.setName("Java Evaluation Project");
        projectDAO.save(project);

        createStudentZip("student_java_pass", "Main.java",
                "public class Main { public static void main(String[] args) { System.out.println(\"Hello \" + args[0]); } }");

        createStudentZip("student_java_fail", "Main.java",
                "public class Main { public static void main(String[] args) { System.out.println(\"Wrong Output\"); } }");

        createStudentZip("student_java_error", "Main.java",
                "public class Main { public static void main(String[] args) { System.out.println(\"Syntax Error\" } }");

        List<EvaluationResult> results = evaluationService.evaluateProject(project);

        assertEquals(3, results.size());
        assertResultStatus(results, "student_java_pass", Status.PASS);
        assertResultStatus(results, "student_java_fail", Status.FAIL);
        assertResultStatus(results, "student_java_error", Status.FAIL);
    }

    @Test
    @DisplayName("Evaluate Python Submissions")
    void testPythonEvaluation() throws Exception {
        String pythonCmd = isCommandAvailable("python") ? "python" : (isCommandAvailable("python3") ? "python3" : null);
        Assumptions.assumeTrue(pythonCmd != null, "Python is not found in PATH");

        Configuration config = new ConfigurationBuilder()
                .setName("Python Config")
                .setLanguage("Python")
                .setFileExtension("py")
                .setCompileCommand("") 
                .setRunCommand(pythonCmd + " main.py {args}")
                .setComparisonStrategy(new TrimLinesStrategy())
                .build();

        Project project = new Project(config, tempSubmissionsDir.toString(), new String[]{"Bob"}, "Hello Bob");
        project.setName("Python Evaluation Project");
        projectDAO.save(project);

        createStudentZip("student_py_pass", "main.py", "import sys\nprint('Hello ' + sys.argv[1])");
        createStudentZip("student_py_fail", "main.py", "print('Hello World')");
        createStudentZip("student_py_error", "main.py", "print('Syntax Error'");

        List<EvaluationResult> results = evaluationService.evaluateProject(project);

        assertEquals(3, results.size());
        assertResultStatus(results, "student_py_pass", Status.PASS);
        assertResultStatus(results, "student_py_fail", Status.FAIL);
        // Runtime execution failures (like Syntax Errors in interpreted languages) cause non-zero exit codes.
        assertResultStatus(results, "student_py_error", Status.FAIL);
    }

    @Test
    @DisplayName("Evaluate C Submissions")
    void testCEvaluation() throws Exception {
        Assumptions.assumeTrue(isCommandAvailable("gcc"), "gcc not found in PATH");

        Configuration config = new ConfigurationBuilder()
                .setName("C Config")
                .setLanguage("C")
                .setFileExtension("c")
                .setCompileCommand("gcc {src} -o {out}")
                .setRunCommand("{out} {args}")
                .setComparisonStrategy(new TrimLinesStrategy())
                .build();

        Project project = new Project(config, tempSubmissionsDir.toString(), new String[]{"Alice"}, "Hello Alice");
        project.setName("C Evaluation Project");
        projectDAO.save(project);

        createStudentZip("student_c_pass", "main.c",
                "#include <stdio.h>\nint main(int argc, char *argv[]) { if(argc > 1) printf(\"Hello %s\\n\", argv[1]); return 0; }");

        createStudentZip("student_c_fail", "main.c",
                "#include <stdio.h>\nint main() { printf(\"Goodbye C\\n\"); return 0; }");

        createStudentZip("student_c_error", "main.c",
                "#include <stdio.h>\nint main() { printf(\"Missing semicolon\\n\") return 0; }");

        List<EvaluationResult> results = evaluationService.evaluateProject(project);

        assertEquals(3, results.size());
        assertResultStatus(results, "student_c_pass", Status.PASS);
        assertResultStatus(results, "student_c_fail", Status.FAIL);
        assertResultStatus(results, "student_c_error", Status.FAIL);
    }

    // --- Utility Methods ---

    private void createStudentZip(String studentId, String fileName, String content) throws IOException {
        File zipFile = new File(tempSubmissionsDir.toFile(), studentId + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes());
            zos.closeEntry();
        }
    }

    private void assertResultStatus(List<EvaluationResult> results, String studentId, Status expectedStatus) {
        EvaluationResult result = results.stream()
                .filter(r -> r.getStudentId().equals(studentId))
                .findFirst()
                .orElse(null);
        assertNotNull(result, "Result not found for student: " + studentId);
        assertEquals(expectedStatus, result.getStatus(), "Status mismatch for student: " + studentId);
    }

    private void clearDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    private boolean isCommandAvailable(String command) {
        try {
            return new ProcessBuilder(command, "--version").start().waitFor() == 0 ||
                   new ProcessBuilder(command, "-version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
