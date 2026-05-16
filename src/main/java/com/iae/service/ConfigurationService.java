package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.evaluation.strategies.ComparisonStrategy;
import com.iae.persistence.ConfigurationPersistenceException;

import java.util.List;


public class ConfigurationService {

    private final ConfigurationManager manager;

    public ConfigurationService() {
        this.manager = ConfigurationManager.getInstance();
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------


    public Configuration createConfiguration(
            String name,
            String language,
            String fileExtension,
            String compileCommand,
            String runCommand,
            ComparisonStrategy comparisonStrategy,
            String description) throws ConfigurationPersistenceException {

        validateNotBlank(name, "Name");
        validateNotBlank(language, "Language");
        validateNotBlank(runCommand, "Run command");
        validateNotNull(comparisonStrategy, "Comparison strategy");

        if (manager.configurationExists(name.trim())) {
            throw new IllegalArgumentException(
                    "A configuration named '" + name.trim() + "' already exists. Use update instead.");
        }

        Configuration config = new ConfigurationBuilder()
                .setName(name.trim())
                .setLanguage(language.trim())
                .setFileExtension(fileExtension == null ? "" : fileExtension.trim())
                .setCompileCommand(compileCommand == null ? "" : compileCommand.trim())
                .setRunCommand(runCommand.trim())
                .setComparisonStrategy(comparisonStrategy)
                .setDescription(description == null ? "" : description.trim())
                .build();

        manager.saveConfiguration(config);
        return config;
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------


    public Configuration getConfiguration(String name) {
        if (name == null || name.isBlank()) return null;
        return manager.getConfiguration(name.trim());
    }


    public List<Configuration> getAllConfigurations() {
        return manager.getAllConfigurations();
    }


    public boolean exists(String name) {
        return name != null && manager.configurationExists(name.trim());
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------


    public Configuration updateConfiguration(
            String oldName,
            String newName,
            String language,
            String fileExtension,
            String compileCommand,
            String runCommand,
            ComparisonStrategy comparisonStrategy,
            String description) throws ConfigurationPersistenceException {

        validateNotBlank(oldName, "Old name");
        validateNotBlank(newName, "New name");
        validateNotBlank(language, "Language");
        validateNotBlank(runCommand, "Run command");
        validateNotNull(comparisonStrategy, "Comparison strategy");

        if (!manager.configurationExists(oldName.trim())) {
            throw new IllegalArgumentException(
                    "Configuration '" + oldName.trim() + "' does not exist.");
        }

        if (!oldName.trim().equals(newName.trim()) && manager.configurationExists(newName.trim())) {
            throw new IllegalArgumentException(
                    "A configuration named '" + newName.trim() + "' already exists.");
        }

        Configuration updated = new ConfigurationBuilder()
                .setName(newName.trim())
                .setLanguage(language.trim())
                .setFileExtension(fileExtension == null ? "" : fileExtension.trim())
                .setCompileCommand(compileCommand == null ? "" : compileCommand.trim())
                .setRunCommand(runCommand.trim())
                .setComparisonStrategy(comparisonStrategy)
                .setDescription(description == null ? "" : description.trim())
                .build();

        manager.renameAndSaveConfiguration(oldName.trim(), updated);
        return updated;
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------


    public void deleteConfiguration(String name) throws ConfigurationPersistenceException {
        validateNotBlank(name, "Name");
        if (!manager.configurationExists(name.trim())) {
            throw new IllegalArgumentException(
                    "Configuration '" + name.trim() + "' does not exist.");
        }
        manager.deleteConfigurationFromDisk(name.trim());
    }

    // -------------------------------------------------------------------------
    // Bulk persistence helpers
    // -------------------------------------------------------------------------


    public void reloadFromDisk() {
        manager.loadConfigurations();
    }


    public void saveAll() throws ConfigurationPersistenceException {
        manager.saveAllConfigurations();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }

    private void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null.");
        }
    }
}
