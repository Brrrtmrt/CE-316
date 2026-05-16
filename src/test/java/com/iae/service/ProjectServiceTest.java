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

    @Test
    void addProjectStoresProjectAndAssignsId() {
        ProjectService projectService = ProjectService.getInstance();

        Configuration configuration = new Configuration(
                "Java 17",
                "Java",
                ".java",
                "javac Main.java",
                "java Main",
                new ExactMatchStrategy(),
                "test"
        );

        Project project = new Project(configuration, ".", new String[0], "");
        project.setName("Sample");

        projectService.addProject(project);

        assertNotNull(project.getId());
        assertSame(project, projectService.getProject(project.getId()));
    }

    @Test
    void addProjectRejectsDuplicateId() {
        ProjectService projectService = ProjectService.getInstance();
        Configuration configuration = new Configuration(
                "Java 17",
                "Java",
                ".java",
                "javac Main.java",
                "java Main",
                new ExactMatchStrategy(),
                "test"
        );

        Project firstProject = new Project(configuration, ".", new String[0], "");
        firstProject.setId("duplicate-id");
        projectService.addProject(firstProject);

        Project duplicateProject = new Project(configuration, ".", new String[0], "");
        duplicateProject.setId("duplicate-id");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> projectService.addProject(duplicateProject));

        assertEquals("Project with ID already exists: duplicate-id", ex.getMessage());
        assertSame(firstProject, projectService.getProject("duplicate-id"));
    }
}
