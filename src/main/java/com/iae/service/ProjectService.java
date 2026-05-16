package com.iae.service;

import com.iae.domain.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProjectService {

    private static final ProjectService instance = new ProjectService();
    private final Map<String, Project> projectCache;

    private ProjectService() {
        this.projectCache = new HashMap<>();
    }

    public static ProjectService getInstance() {
        return instance;
    }

    public void addProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }

        if (project.getId() == null || project.getId().trim().isEmpty()) {
            project.setId(UUID.randomUUID().toString());
        }

        projectCache.put(project.getId(), project);
    }

    public Project getProject(String projectId) {
        return projectCache.get(projectId);
    }

    public List<Project> getAllProjects() {
        return new ArrayList<>(projectCache.values());
    }
}
