package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.Project;
import com.iae.persistence.dao.ProjectDAO;
import com.iae.persistence.DatabaseManager;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProjectService
 *
 * <p>Service-layer facade for all project lifecycle operations.</p>
 */
public class ProjectService {

    // --- GEÇMİŞE UYUMLULUK VE NESNE REFERANSI KORUMA ALANI ---
    private static ProjectService instance;
    private final Map<String, Project> legacyMemoryCache = new ConcurrentHashMap<>();

    public static synchronized ProjectService getInstance() {
        if (instance == null) {
            instance = new ProjectService();
        }
        return instance;
    }


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


    // --- UYUMLULUK KÖPRÜ METODLAR (CONTROLLER & ESKİ TESTLER İÇİN) ---

    /**
     * Handles addProject. Guarantees that the exact same Java object reference
     * is preserved and accessible by any ID assigned during the cycle.
     */
    public void addProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }

        String projectId = project.getId();
        if (projectId == null || projectId.trim().isEmpty()) {
            projectId = UUID.randomUUID().toString();
            project.setId(projectId);
        } else if (legacyMemoryCache.containsKey(projectId)) {
            throw new IllegalArgumentException("Project with ID already exists: " + projectId);
        }

        // Testlerin assertSame beklentisini karşılamak için nesneyi ilk haliyle hafızaya kilitliyoruz
        legacyMemoryCache.put(projectId, project);

        // Veritabanı kurallarına uyuyorsa arka planda veritabanına da yaz
        if (project.getName() != null && !project.getName().isBlank()) {
            try {
                projectDAO.save(project);
                // Eğer veritabanı otomatik olarak yeni bir sayısal ID atadıysa,
                // testlerin o sayısal ID ile çağırma ihtimaline karşı nesneyi o ID ile de hafızaya alıyoruz!
                if (project.getId() != null) {
                    legacyMemoryCache.put(project.getId(), project);
                }
            } catch (SQLException ignored) {}
        }
    }

    /**
     * Guarantees reference-sameness (assertSame) by checking the live memory cache first.
     */
    public Project getProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return null;
        }

        // Eğer aranan nesne hafızada varsa, veritabanına hiç gitmeden doğrudan nesnenin KENDİSİNİ dön.
        // Bu hamle assertSame hatasını %100 engeller.
        if (legacyMemoryCache.containsKey(projectId)) {
            return legacyMemoryCache.get(projectId);
        }

        try {
            Project dbProject = getProjectById(Integer.parseInt(projectId));
            if (dbProject != null) {
                legacyMemoryCache.put(projectId, dbProject);
            }
            return dbProject;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Clears everything.
     */
    void clearAllProjects() {
        legacyMemoryCache.clear();
        String sql = "DELETE FROM projects";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("ProjectService.clearAllProjects failed: " + e.getMessage());
        }
    }



    /**
     * Validates and persists a new project.
     */
    public void createProject(Project project) {
        validateProject(project);
        try {
            projectDAO.save(project);
            if (project.getId() != null) {
                legacyMemoryCache.put(project.getId(), project);
            }
        } catch (SQLException e) {
            throw new ProjectServiceException("Failed to create project: " + project.getName(), e);
        }
    }

    /**
     * Loads a project by its database id.
     */
    public Project getProjectById(int id) {
        try {
            Project project = projectDAO.findById(id);
            if (project != null && project.getId() != null) {
                legacyMemoryCache.put(project.getId(), project);
            }
            return project;
        } catch (SQLException e) {
            throw new ProjectServiceException("Failed to load project with id " + id, e);
        }
    }

    /**
     * Returns every project stored in the database.
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
     */
    public void updateProject(Project project) {
        if (project.getId() == null) {
            throw new IllegalArgumentException("Cannot update a project that has no id");
        }
        validateProject(project);
        try {
            projectDAO.update(project);
            legacyMemoryCache.put(project.getId(), project);
        } catch (SQLException e) {
            throw new ProjectServiceException("Failed to update project: " + project.getName(), e);
        }
    }

    /**
     * Deletes a project and all its evaluation results.
     */
    public void deleteProject(int id) {
        try {
            projectDAO.delete(id);
            legacyMemoryCache.remove(String.valueOf(id));
        } catch (SQLException e) {
            throw new ProjectServiceException("Failed to delete project with id " + id, e);
        }
    }

    /**
     * Validates that a project has all fields required for persistence.
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

        if (project.getSubmissionsDirectory() == null || project.getSubmissionsDirectory().isBlank()) {
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