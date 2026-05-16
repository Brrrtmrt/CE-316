package com.iae.domain;

import java.time.LocalDateTime;

public class Project {
    private Configuration configuration;
    private String submissionsDirectory;
    private String[] programArguments;
    private String expectedOutput;
    private String id;
    private String name;
    private LocalDateTime lastRunDate;

    public Project(Configuration configuration, String submissionsDirectory, String[] programArguments, String expectedOutput) {
        this.configuration = configuration;
        this.submissionsDirectory = submissionsDirectory;
        this.programArguments = programArguments;
        this.expectedOutput = expectedOutput;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getSubmissionsDirectory() {
        return submissionsDirectory;
    }

    public String[] getProgramArguments() {
        return programArguments;
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
}