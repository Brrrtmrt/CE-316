package com.iae.service;

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
