package com.iae.domain;

import com.iae.evaluation.strategies.ComparisonStrategy;

public class Configuration {
    private String name;
    private String language;
    private String fileExtension;
    private String compileCommand;
    private String runCommand;
    private ComparisonStrategy comparisonStrategy;
    private String description;

    public Configuration(String name, String language, String fileExtension, String compileCommand, String runCommand, ComparisonStrategy comparisonStrategy, String description) {
        this.name = name;
        this.language = language;
        this.fileExtension = fileExtension;
        this.compileCommand = compileCommand;
        this.runCommand = runCommand;
        this.comparisonStrategy = comparisonStrategy;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public ComparisonStrategy getComparisonStrategy() {
        return comparisonStrategy;
    }

    public String getCompileCommand() {
        return compileCommand;
    }

    public String getLanguage() {
        return language;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getRunCommand() {
        return runCommand;
    }

    public String getDescription() {
        return description;
    }
}