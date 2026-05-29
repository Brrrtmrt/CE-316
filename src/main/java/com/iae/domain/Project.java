package com.iae.domain;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public class Project {
    private final Configuration configuration;
    private final String submissionsDirectory;
    private final String[] programArguments;
    private final String expectedOutput;
    private String id;
    private String name;
    private LocalDateTime lastRunDate;

    public Project(Configuration configuration, String submissionsDirectory, String[] programArguments, String expectedOutput) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (submissionsDirectory == null) {
            throw new IllegalArgumentException("Submissions directory cannot be null");
        }
        if (expectedOutput == null) {
            throw new IllegalArgumentException("Expected output cannot be null");
        }
        this.configuration = configuration;
        this.submissionsDirectory = submissionsDirectory;
        this.programArguments = programArguments == null ? new String[0] : Arrays.copyOf(programArguments, programArguments.length);
        this.expectedOutput = expectedOutput;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getSubmissionsDirectory() {
        return submissionsDirectory;
    }

    public String[] getProgramArguments() {
        return Arrays.copyOf(programArguments, programArguments.length);
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getLastRunDate() {
        return lastRunDate;
    }

    public void setLastRunDate(LocalDateTime lastRunDate) {
        this.lastRunDate = lastRunDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Project{id='" + id + "', name='" + name + "', submissionsDirectory='" + submissionsDirectory + "'}";
    }
}
