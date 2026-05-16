package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.evaluation.strategies.ComparisonStrategy;
import com.iae.persistence.ConfigurationPersistenceException;

import java.util.List;

/**
 * High-level CRUD service for {@link Configuration} objects.
 *
 * <p>This class acts as the single entry-point for all configuration
 * operations in the application.  It validates input, delegates
 * persistence to {@link ConfigurationManager}, and wraps errors in
 * meaningful messages so callers (controllers, tests) do not have to
 * know about the storage layer.
 *
 * <p>Sprint 2 scope: create, read, update, delete, list.
 */
public class ConfigurationService {

    private final ConfigurationManager manager;

    public ConfigurationService() {
        this.manager = ConfigurationManager.getInstance();
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Creates a new configuration and saves it to disk.
     *
     * @param name               unique display name (must not be blank or duplicate;
     *                           only letters, digits, dot, underscore, hyphen, space allowed)
     * @param language           programming language label (must not be blank)
     * @param fileExtension      student submission file extension, e.g. ".java".
     *                           May be empty when no extension is required.
     * @param compileCommand     shell command used to compile, or {@code ""} for interpreted languages
     * @param runCommand         shell command used to run (must not be blank)
     * @param comparisonStrategy output comparison strategy (must not be null)
     * @param description        optional human-readable note
     * @return the created {@link Configuration}
     * @throws IllegalArgumentException          if validation fails or the name already exists
     * @throws ConfigurationPersistenceException if persistence fails
     */
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

    /**
     * Replaces the configuration identified by {@code oldName} with a new one
     * built from the supplied values, then persists the change.
     *
     * <p>If the name is changing, the old JSON file on disk is deleted to
     * prevent an orphan file from being re-loaded on the next startup.
     *
     * @param oldName            the name of the configuration to replace
     * @param newName            the new name (may equal oldName for in-place update)
     * @param comparisonStrategy must not be null
     * @throws IllegalArgumentException          if validation fails or {@code oldName} does not exist
     * @throws ConfigurationPersistenceException if persistence fails
     */
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
