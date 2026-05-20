package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.Project;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.persistence.DatabaseManager;
import com.iae.persistence.dao.ProjectDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ProjectServiceTest {

    private File tempDbFile;
    private ProjectService projectService;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("iae_project_service_test_", ".db");
        tempDbFile.deleteOnExit();

        DatabaseManager.setDbUrl(
                "jdbc:sqlite:" + tempDbFile.getAbsolutePath().replace("\\", "/"));
        DatabaseManager.initializeDatabase();

        projectService = new ProjectService(new ProjectDAO());
        projectService.clearAllProjects();
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.resetDbUrl();
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private Configuration buildConfiguration() {
        return new Configuration(
                "Java 17",
                "Java",
                ".java",
                "javac Main.java",
                "java Main",
                new ExactMatchStrategy(),
                "test"
        );
    }

    @Test
    void addProjectStoresProjectAndAssignsId() {
        Project project = new Project(buildConfiguration(), ".", new String[0], "Hello World\n");
        project.setName("Sample");

        projectService.addProject(project);

        assertNotNull(project.getId());

        Project loaded = projectService.getProject(project.getId());
        assertNotNull(loaded);
        assertEquals(project.getId(), loaded.getId());
        assertEquals("Sample", loaded.getName());
    }

    @Test
    void addProjectRejectsDuplicateId() {
        Project firstProject = new Project(buildConfiguration(), ".", new String[0], "Hello World\n");
        firstProject.setName("First");
        projectService.addProject(firstProject);

        String assignedId = firstProject.getId();

        Project duplicateProject = new Project(buildConfiguration(), ".", new String[0], "Hello World\n");
        duplicateProject.setName("Duplicate");
        duplicateProject.setId(assignedId);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> projectService.addProject(duplicateProject));

        assertEquals("Project with ID already exists: " + assignedId, ex.getMessage());

        Project loaded = projectService.getProject(assignedId);
        assertNotNull(loaded);
        assertEquals("First", loaded.getName());
    }
}
