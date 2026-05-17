package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.Project;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectServiceTest {

    @BeforeEach
    void setUp() {
        ProjectService.getInstance().clearAllProjects();
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
        ProjectService projectService = ProjectService.getInstance();

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
        ProjectService projectService = ProjectService.getInstance();

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