package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.Project;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectServiceTest {

    @Test
    void addProjectStoresProjectAndAssignsId() {
        ProjectService projectService = ProjectService.getInstance();
        int initialSize = projectService.getAllProjects().size();

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
        assertEquals(initialSize + 1, projectService.getAllProjects().size());
        assertSame(project, projectService.getProject(project.getId()));
    }
}
