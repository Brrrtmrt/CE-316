package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.evaluation.strategies.ComparisonStrategy;

import java.util.List;


public class ConfigurationService {

    private final ConfigurationManager manager;

    public ConfigurationService() {
        this.manager = ConfigurationManager.getInstance();
    }




    public Configuration createConfiguration(
            String name,
            String language,
            String fileExtension,
            String compileCommand,
            String runCommand,
            ComparisonStrategy comparisonStrategy,
            String description) throws Exception {

        validateNotBlank(name, "Name");
        validateNotBlank(language, "Language");
        validateNotBlank(runCommand, "Run command");

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


    public Configuration updateConfiguration(
            String oldName,
            String newName,
            String language,
            String fileExtension,
            String compileCommand,
            String runCommand,
            ComparisonStrategy comparisonStrategy,
            String description) throws Exception {

        validateNotBlank(oldName, "Old name");
        validateNotBlank(newName, "New name");
        validateNotBlank(language, "Language");
        validateNotBlank(runCommand, "Run command");

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

        // Rename-aware save: writes new file AND deletes old file if name changed
        manager.renameAndSaveConfiguration(oldName.trim(), updated);
        return updated;
    }


    public void deleteConfiguration(String name) throws Exception {
        validateNotBlank(name, "Name");
        if (!manager.configurationExists(name.trim())) {
            throw new IllegalArgumentException(
                    "Configuration '" + name.trim() + "' does not exist.");
        }
        manager.deleteConfigurationFromDisk(name.trim());
    }


    public void reloadFromDisk() {
        manager.loadConfigurations();
    }


    public void saveAll() throws Exception {
        manager.saveAllConfigurations();
    }



    private void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
