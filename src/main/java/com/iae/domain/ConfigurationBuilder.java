package com.iae.domain;

import com.iae.evaluation.strategies.ComparisonStrategy;
import com.iae.evaluation.strategies.ExactMatchStrategy;

public class ConfigurationBuilder {

    private String name;
    private String language;
    private String fileExtension;
    private String compileCommand;
    private String runCommand;
    private ComparisonStrategy comparisonStrategy;
    private String description;

    public ConfigurationBuilder() {
        // Set defaults
        this.comparisonStrategy = new ExactMatchStrategy();
    }

    public ConfigurationBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ConfigurationBuilder setLanguage(String language) {
        this.language = language;
        return this;
    }

    public ConfigurationBuilder setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

    public ConfigurationBuilder setCompileCommand(String compileCommand) {
        this.compileCommand = compileCommand;
        return this;
    }

    public ConfigurationBuilder setRunCommand(String runCommand) {
        this.runCommand = runCommand;
        return this;
    }

    public ConfigurationBuilder setComparisonStrategy(ComparisonStrategy comparisonStrategy) {
        this.comparisonStrategy = comparisonStrategy;
        return this;
    }

    public ConfigurationBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public Configuration build() {
        validate();
        return new Configuration(name, language, fileExtension, compileCommand,
                runCommand, comparisonStrategy, description);
    }

    private void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Configuration name is required");
        }
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalStateException("Language is required");
        }
        if (comparisonStrategy == null) {
            throw new IllegalStateException("Comparison strategy is required");
        }
    }
}
