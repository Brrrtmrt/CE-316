package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.Project;
import com.iae.domain.EvaluationResult;
import com.iae.persistence.dao.ProjectDAO;
import com.iae.persistence.DatabaseManager;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * ProjectService
 *
 * <p>Service-layer facade for all project lifecycle operations.
 * Coordinates between {@link ProjectDAO} (persistence) and the rest of
 * the application.  Controllers should talk to this class, never to
 * {@link ProjectDAO} directly.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Create, read, update, delete projects</li>
 *   <li>Validate project state before persistence</li>
 *   <li>Ensure the database is initialised before first use</li>
 * </ul>
 *
 * <h2>MT-Unsafe</h2>
 * <p>One instance per session — do not share across threads.</p>
 *
 * @author Dev 1
 * @version 1.0
 */
public class ProjectService {

    private final ProjectDAO projectDAO;



    /** Production constructor — initialises the database on first call. */
    public ProjectService() {
        this.projectDAO = new ProjectDAO();
        ensureDatabaseReady();
    }

    /** Test constructor — accepts an injected DAO (e.g. a mock). */
    public ProjectService(ProjectDAO projectDAO) {
        this.projectDAO = projectDAO;
    }



    /**
     * Validates and persists a new project.
     *
     * <p>The project's {@code id} field is set on return.</p>
     *
     * @param project the project to save
     * @throws IllegalArgumentException if validation fails
     * @throws ProjectServiceException  if the database operation fails
     */
    public void createProject(Project project) {
        validateProject(project);
        try {
            projectDAO.save(project);
        } catch (SQLException e) {
            throw new ProjectServiceException("Failed to create project: " + project.getName(), e);
        }
    }



    /**
     * Loads a project by its database id.
     *
     * @param id the project's integer primary key
     * @return the project, or {@code null} if not found
     * @throws ProjectServiceException if the database operation fails
     */
    public Project getProjectById(int id) {
        try {
            return projectDAO.findById(id);
        } catch (SQLException e) {
            throw new ProjectServiceException("Failed to load project with id " + id, e);
        }
    }

    /**
     * Returns every project stored in the database.
     *
     * @return list of projects; empty if none exist
     * @throws ProjectServiceException if the database operation fails
     */
    public List<Project> getAllProjects() {
        try {
            return projectDAO.findAll();
        } catch (SQLException e) {
            throw new ProjectServiceException("Failed to load projects", e);
        }
    }


    /**
     * Validates and updates an existing project.
     *
     * @param project the project with updated fields (must have a non-null id)
     * @throws IllegalArgumentException if validation fails or id is missing
     * @throws ProjectServiceException  if the database operation fails
     */
    public void updateProject(Project project) {
        if (project.getId() == null) {
            throw new IllegalArgumentException("Cannot update a project that has no id");
        }
        validateProject(project);
        try {
            projectDAO.update(project);
        } catch (SQLException e) {
            throw new ProjectServiceException("Failed to update project: " + project.getName(), e);
        }
    }


    /**
     * Deletes a project and all its evaluation results (cascade handled by FK).
     *
     * @param id the project's database id
     * @throws ProjectServiceException if the database operation fails
     */
    public void deleteProject(int id) {
        try {
            projectDAO.delete(id);
        } catch (SQLException e) {
            throw new ProjectServiceException("Failed to delete project with id " + id, e);
        }
    }


    /**
     * Validates that a project has all fields required for persistence and
     * evaluation.
     *
     * @param project the project to validate
     * @throws IllegalArgumentException on the first validation failure found
     */
    private void validateProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        if (project.getName() == null || project.getName().isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }

        Configuration cfg = project.getConfiguration();
        if (cfg == null) {
            throw new IllegalArgumentException("Project must have a configuration");
        }
        if (cfg.getName() == null || cfg.getName().isBlank()) {
            throw new IllegalArgumentException("Configuration name is required");
        }
        if (cfg.getLanguage() == null || cfg.getLanguage().isBlank()) {
            throw new IllegalArgumentException("Configuration language is required");
        }
        if (cfg.getFileExtension() == null || cfg.getFileExtension().isBlank()) {
            throw new IllegalArgumentException("Configuration file extension is required");
        }
        if (cfg.getRunCommand() == null || cfg.getRunCommand().isBlank()) {
            throw new IllegalArgumentException("Configuration run command is required");
        }
        if (cfg.getComparisonStrategy() == null) {
            throw new IllegalArgumentException("Configuration must have a comparison strategy");
        }

        if (project.getSubmissionsDirectory() == null
                || project.getSubmissionsDirectory().isBlank()) {
            throw new IllegalArgumentException("Submissions directory is required");
        }
        File submissionsDir = new File(project.getSubmissionsDirectory());
        if (!submissionsDir.exists() || !submissionsDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "Submissions directory does not exist: " + project.getSubmissionsDirectory());
        }

        if (project.getExpectedOutput() == null || project.getExpectedOutput().isBlank()) {
            throw new IllegalArgumentException("Expected output is required");
        }
    }


    private void ensureDatabaseReady() {
        try {
            DatabaseManager.initializeDatabase();
        } catch (Exception e) {
            throw new ProjectServiceException("Failed to initialise database", e);
        }
    }


    public static class ProjectServiceException extends RuntimeException {
        public ProjectServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
import com.iae.domain.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectService {

    private static final ProjectService instance = new ProjectService();
    private final Map<String, Project> projects;

    private ProjectService() {
        this.projects = new ConcurrentHashMap<>();
    }

    public static ProjectService getInstance() {
        return instance;
    }

    public void addProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }

        String projectId = project.getId();
        if (projectId == null || projectId.trim().isEmpty()) {
            projectId = UUID.randomUUID().toString();
            project.setId(projectId);
        } else if (projects.containsKey(projectId)) {
            throw new IllegalArgumentException("Project with ID already exists: " + projectId);
        }

        projects.put(projectId, project);
    }

    public Project getProject(String projectId) {
        return projects.get(projectId);
    }

    public List<Project> getAllProjects() {
        return new ArrayList<>(projects.values());
    }

    void clearAllProjects() {
        projects.clear();
    }
}
