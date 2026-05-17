package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.domain.EvaluationResult;
import com.iae.domain.Project;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.evaluation.strategies.IgnoreWhitespaceStrategy;
import com.iae.evaluation.strategies.TrimLinesStrategy;
import com.iae.persistence.DatabaseManager;
import com.iae.persistence.dao.ProjectDAO;
import com.iae.persistence.dao.ResultDAO;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectServiceIntegrationTest {

    private static Path tempSubmissionsDir;
    private File tempDbFile;

    private ProjectService projectService;
    private ProjectDAO     projectDAO;
    private ResultDAO      resultDAO;

    @BeforeAll
    static void createSubmissionsDir() throws IOException {
        tempSubmissionsDir = Files.createTempDirectory("iae_test_submissions_");
    }

    @AfterAll
    static void removeSubmissionsDir() throws IOException {
        Files.deleteIfExists(tempSubmissionsDir);
    }

    @BeforeEach
    void setUp() throws Exception {

        tempDbFile = File.createTempFile("iae_integration_test_", ".db");
        tempDbFile.deleteOnExit();

        String testDbUrl = "jdbc:sqlite:" + tempDbFile.getAbsolutePath().replace("\\", "/");


        DatabaseManager.setDbUrl(testDbUrl);


        DatabaseManager.initializeDatabase();


        this.projectDAO     = new ProjectDAO();
        this.resultDAO      = new ResultDAO();
        this.projectService = new ProjectService(projectDAO);
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.resetDbUrl();
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private Project buildProject(String name, String language, String fileExt,
                                 String compileCmd, String runCmd, String strategy) {
        ConfigurationBuilder cb = new ConfigurationBuilder()
                .setName(name + " Config")
                .setLanguage(language)
                .setFileExtension(fileExt)
                .setCompileCommand(compileCmd)
                .setRunCommand(runCmd)
                .setDescription(language + " test configuration");

        switch (strategy) {
            case "ignore_whitespace" -> cb.setComparisonStrategy(new IgnoreWhitespaceStrategy());
            case "trim_lines"        -> cb.setComparisonStrategy(new TrimLinesStrategy());
            default                  -> cb.setComparisonStrategy(new ExactMatchStrategy());
        }

        Project p = new Project(
                cb.build(),
                tempSubmissionsDir.toString(),
                new String[]{"arg1", "arg2"},
                "Hello World\n"
        );
        p.setName(name);
        return p;
    }

    private Project buildJavaProject(String name) {
        return buildProject(name, "Java", ".java", "javac -d out {src}", "java -cp out Main {args}", "exact");
    }

    private Project buildCProject(String name) {
        return buildProject(name, "C", ".c", "gcc {src} -o {out}", "{out} {args}", "trim_lines");
    }

    private Project buildPythonProject(String name) {
        return buildProject(name, "Python", ".py", null, "python3 {src} {args}", "ignore_whitespace");
    }

    //  Core Sprint Integration Tests

    @Test
    @Order(1)
    @DisplayName("createProject assigns a non-null id after save")
    void createProject_assignsId() throws SQLException {
        Project p = buildJavaProject("MyProject");
        assertNull(p.getId());

        projectDAO.save(p);

        assertNotNull(p.getId());
        assertFalse(p.getId().isBlank());
    }

    @Test
    @Order(2)
    @DisplayName("findById returns the saved project with correct fields")
    void findById_roundTrip() throws SQLException {
        Project original = buildJavaProject("RoundTripProject");
        projectDAO.save(original);

        int id = Integer.parseInt(original.getId());
        Project loaded = projectDAO.findById(id);

        assertNotNull(loaded);
        assertEquals("RoundTripProject", loaded.getName());
        assertEquals("Java", loaded.getConfiguration().getLanguage());
        assertEquals(tempSubmissionsDir.toString(), loaded.getSubmissionsDirectory());
    }

    @Test
    @Order(3)
    @DisplayName("findAll returns all saved projects")
    void findAll_returnsAllProjects() throws SQLException {
        projectDAO.save(buildJavaProject("Project A"));
        projectDAO.save(buildCProject("Project B"));
        projectDAO.save(buildPythonProject("Project C"));

        List<Project> all = projectDAO.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @Order(4)
    @DisplayName("findById returns null for a non-existent id")
    void findById_notFound_returnsNull() throws SQLException {
        Project result = projectDAO.findById(99999);
        assertNull(result);
    }

    @Test
    @Order(5)
    @DisplayName("update persists changed fields correctly")
    void update_changesArePersisted() throws SQLException {
        Project p = buildJavaProject("OriginalName");
        projectDAO.save(p);

        p.setName("UpdatedName");
        projectDAO.update(p);

        Project loaded = projectDAO.findById(Integer.parseInt(p.getId()));
        assertNotNull(loaded);
        assertEquals("UpdatedName", loaded.getName());
    }

    @Test
    @Order(6)
    @DisplayName("delete removes the project and findById returns null")
    void delete_removesProject() throws SQLException {
        Project p = buildCProject("ToBeDeleted");
        projectDAO.save(p);
        int id = Integer.parseInt(p.getId());

        projectDAO.delete(id);
        assertNull(projectDAO.findById(id));
    }

    @Test
    @Order(7)
    @DisplayName("ExactMatchStrategy survives save/load round-trip")
    void strategy_exactMatch_roundTrip() throws SQLException {
        Project p = buildJavaProject("ExactProject");
        projectDAO.save(p);

        Project loaded = projectDAO.findById(Integer.parseInt(p.getId()));
        assertNotNull(loaded);
        assertInstanceOf(ExactMatchStrategy.class, loaded.getConfiguration().getComparisonStrategy());
    }

    @Test
    @Order(8)
    @DisplayName("TrimLinesStrategy survives save/load round-trip")
    void strategy_trimLines_roundTrip() throws SQLException {
        Project p = buildCProject("TrimProject");
        projectDAO.save(p);

        Project loaded = projectDAO.findById(Integer.parseInt(p.getId()));
        assertNotNull(loaded);
        assertInstanceOf(TrimLinesStrategy.class, loaded.getConfiguration().getComparisonStrategy());
    }

    @Test
    @Order(9)
    @DisplayName("IgnoreWhitespaceStrategy survives save/load round-trip")
    void strategy_ignoreWhitespace_roundTrip() throws SQLException {
        Project p = buildPythonProject("WhitespaceProject");
        projectDAO.save(p);

        Project loaded = projectDAO.findById(Integer.parseInt(p.getId()));
        assertNotNull(loaded);
        assertInstanceOf(IgnoreWhitespaceStrategy.class, loaded.getConfiguration().getComparisonStrategy());
    }

    @Test
    @Order(10)
    @DisplayName("ResultDAO.save persists all boolean fields correctly")
    void resultDAO_save_persistsFields() throws SQLException {
        Project p = buildJavaProject("ResultProject");
        projectDAO.save(p);

        EvaluationResult result = new EvaluationResult("student001");
        result.setUnzipSuccess(true);
        result.setCompileSuccess(true);
        result.setRunSuccess(true);
        result.setOutputMatch(true);

        resultDAO.save(p.getId(), result);

        EvaluationResult loaded = resultDAO.findByProjectAndStudent(p.getId(), "student001");
        assertNotNull(loaded);
        assertTrue(loaded.isUnzipSuccess());
        assertTrue(loaded.isCompileSuccess());
    }

    @Test
    @Order(11)
    @DisplayName("ResultDAO.findByProjectId returns all results for a project")
    void resultDAO_findByProjectId_returnsAll() throws SQLException {
        Project p = buildJavaProject("MultiStudentProject");
        projectDAO.save(p);

        for (int i = 1; i <= 3; i++) {
            EvaluationResult r = new EvaluationResult("student00" + i);
            r.setUnzipSuccess(true);
            resultDAO.save(p.getId(), r);
        }

        List<EvaluationResult> results = resultDAO.findByProjectId(p.getId());
        assertEquals(3, results.size());
    }

    @Test
    @Order(12)
    @DisplayName("ResultDAO.save uses INSERT OR REPLACE — re-run overwrites previous result")
    void resultDAO_save_overwritesPreviousResult() throws SQLException {
        Project p = buildJavaProject("OverwriteProject");
        projectDAO.save(p);

        EvaluationResult first = new EvaluationResult("student001");
        first.setCompileSuccess(false);
        resultDAO.save(p.getId(), first);

        EvaluationResult second = new EvaluationResult("student001");
        second.setCompileSuccess(true);
        resultDAO.save(p.getId(), second);

        List<EvaluationResult> results = resultDAO.findByProjectId(p.getId());
        assertEquals(1, results.size());
        assertTrue(results.get(0).isCompileSuccess());
    }

    @Test
    @Order(13)
    @DisplayName("ResultDAO.deleteByProjectId removes all results for that project")
    void resultDAO_deleteByProjectId() throws SQLException {
        Project p = buildJavaProject("DeleteResultsProject");
        projectDAO.save(p);

        EvaluationResult r = new EvaluationResult("student001");
        resultDAO.save(p.getId(), r);

        resultDAO.deleteByProjectId(p.getId());
        assertTrue(resultDAO.findByProjectId(p.getId()).isEmpty());
    }

    @Test
    @Order(14)
    @DisplayName("Cascade delete: removing a project removes its evaluation results")
    void cascadeDelete_removesResults() throws SQLException {
        Project p = buildJavaProject("CascadeProject");
        projectDAO.save(p);

        EvaluationResult r = new EvaluationResult("student001");
        resultDAO.save(p.getId(), r);

        projectDAO.delete(Integer.parseInt(p.getId()));
        assertTrue(resultDAO.findByProjectId(p.getId()).isEmpty());
    }

    @Test
    @Order(15)
    @DisplayName("ResultDAO.findByProjectAndStudent returns null when not found")
    void resultDAO_findByProjectAndStudent_notFound() throws SQLException {
        Project p = buildJavaProject("EmptyProject");
        projectDAO.save(p);

        assertNull(resultDAO.findByProjectAndStudent(p.getId(), "ghost_student"));
    }

    @Test
    @Order(16)
    @DisplayName("ProjectService.createProject throws on null project")
    void projectService_createProject_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> projectService.createProject(null));
    }

    @Test
    @Order(17)
    @DisplayName("ProjectService.createProject throws when name is blank")
    void projectService_createProject_blankNameThrows() {
        Project p = buildJavaProject("");
        p.setName("  ");
        assertThrows(IllegalArgumentException.class, () -> projectService.createProject(p));
    }

    @Test
    @Order(18)
    @DisplayName("ProjectService.updateProject throws when id is null")
    void projectService_updateProject_noIdThrows() {
        Project p = buildJavaProject("NoId");
        assertThrows(IllegalArgumentException.class, () -> projectService.updateProject(p));
    }

    @Test
    @Order(19)
    @DisplayName("ProjectService full cycle: create → getById → update → delete")
    void projectService_fullCycle() throws SQLException {
        Project p = buildCProject("CycleProject");

        projectService.createProject(p);
        assertNotNull(p.getId());

        Project loaded = projectService.getProjectById(Integer.parseInt(p.getId()));
        assertNotNull(loaded);

        loaded.setName("CycleProjectRenamed");
        projectService.updateProject(loaded);

        Project updated = projectService.getProjectById(Integer.parseInt(loaded.getId()));
        assertEquals("CycleProjectRenamed", updated.getName());

        projectService.deleteProject(Integer.parseInt(updated.getId()));
        assertNull(projectService.getProjectById(Integer.parseInt(updated.getId())));
    }

    @Test
    @Order(20)
    @DisplayName("ProjectService.getAllProjects returns empty list when no projects exist")
    void projectService_getAllProjects_emptyDatabase() {
        List<Project> all = projectService.getAllProjects();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }
}