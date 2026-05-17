package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.Project;
import com.iae.persistence.DatabaseManager;
import com.iae.persistence.dao.ProjectDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ProjectService {

    private static ProjectService instance;
    private final ProjectDAO projectDAO;

    public static synchronized ProjectService getInstance() {
        if (instance == null) {
            instance = new ProjectService();
        }
        return instance;
    }

    public ProjectService() {
        this.projectDAO = new ProjectDAO();
        ensureDatabaseReady();
    }

    public ProjectService(ProjectDAO projectDAO) {
        this.projectDAO = projectDAO;
    }

    public void addProject(Project project) {
        validateProject(project);

        try {
            if (project.getId() != null && !project.getId().isBlank()) {

                Project existing =
                        projectDAO.findById(
                                Integer.parseInt(project.getId())
                        );

                if (existing != null) {
                    throw new IllegalArgumentException(
                            "Project with ID already exists: "
                                    + project.getId()
                    );
                }
            }

            projectDAO.save(project);

        } catch (SQLException e) {
            throw new ProjectServiceException(
                    "Failed to save project to database",
                    e
            );
        }
    }

    public Project getProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return null;
        }

        try {
            return projectDAO.findById(
                    Integer.parseInt(projectId)
            );

        } catch (SQLException e) {
            throw new ProjectServiceException(
                    "Failed to load project",
                    e
            );

        } catch (NumberFormatException e) {
            return null;
        }
    }

    void clearAllProjects() {
        String sql = "DELETE FROM projects";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            System.err.println(
                    "ProjectService.clearAllProjects failed: "
                            + e.getMessage()
            );
        }
    }

    public void createProject(Project project) {
        validateProject(project);

        try {
            projectDAO.save(project);

        } catch (SQLException e) {
            throw new ProjectServiceException(
                    "Failed to create project: "
                            + project.getName(),
                    e
            );
        }
    }

    public Project getProjectById(int id) {
        try {
            return projectDAO.findById(id);

        } catch (SQLException e) {
            throw new ProjectServiceException(
                    "Failed to load project with id "
                            + id,
                    e
            );
        }
    }

    public List<Project> getAllProjects() {
        try {
            return projectDAO.findAll();

        } catch (SQLException e) {
            throw new ProjectServiceException(
                    "Failed to load projects",
                    e
            );
        }
    }

    public void updateProject(Project project) {
        if (project.getId() == null ||
                project.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "Cannot update a project that has no id"
            );
        }

        validateProject(project);

        try {
            projectDAO.update(project);

        } catch (SQLException e) {
            throw new ProjectServiceException(
                    "Failed to update project: "
                            + project.getName(),
                    e
            );
        }
    }

    public void deleteProject(int id) {
        try {
            projectDAO.delete(id);

        } catch (SQLException e) {
            throw new ProjectServiceException(
                    "Failed to delete project with id "
                            + id,
                    e
            );
        }
    }

    private void validateProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException(
                    "Project cannot be null"
            );
        }

        if (project.getName() == null ||
                project.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Project name is required"
            );
        }

        Configuration cfg = project.getConfiguration();

        if (cfg == null) {
            throw new IllegalArgumentException(
                    "Project must have a configuration"
            );
        }

        if (cfg.getName() == null ||
                cfg.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Configuration name is required"
            );
        }

        if (cfg.getLanguage() == null ||
                cfg.getLanguage().isBlank()) {

            throw new IllegalArgumentException(
                    "Configuration language is required"
            );
        }

        if (cfg.getFileExtension() == null ||
                cfg.getFileExtension().isBlank()) {

            throw new IllegalArgumentException(
                    "Configuration file extension is required"
            );
        }

        if (cfg.getRunCommand() == null ||
                cfg.getRunCommand().isBlank()) {

            throw new IllegalArgumentException(
                    "Configuration run command is required"
            );
        }

        if (cfg.getComparisonStrategy() == null) {
            throw new IllegalArgumentException(
                    "Configuration must have a comparison strategy"
            );
        }

        if (project.getSubmissionsDirectory() == null ||
                project.getSubmissionsDirectory().isBlank()) {

            throw new IllegalArgumentException(
                    "Submissions directory is required"
            );
        }

        if (project.getExpectedOutput() == null ||
                project.getExpectedOutput().isBlank()) {

            throw new IllegalArgumentException(
                    "Expected output is required"
            );
        }
    }

    private void ensureDatabaseReady() {
        try {
            DatabaseManager.initializeDatabase();

        } catch (Exception e) {
            throw new ProjectServiceException(
                    "Failed to initialise database",
                    e
            );
        }
    }

    public static class ProjectServiceException
            extends RuntimeException {

        public ProjectServiceException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }
}