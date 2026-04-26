package com.iae.domain;

public class Project {
    private Configuration configuration;
    private String submissionsDirectory;
    private String[] programArguments;
    private String expectedOutput;

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
}