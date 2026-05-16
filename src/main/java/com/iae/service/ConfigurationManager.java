package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.persistence.ConfigurationIO;
import com.iae.persistence.ConfigurationPersistenceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ConfigurationManager {

    private static final ConfigurationManager instance = new ConfigurationManager();

    private final Map<String, Configuration> configurationCache;
    private final ConfigurationIO configurationIO;

    private ConfigurationManager() {
        this.configurationCache = new HashMap<>();
        this.configurationIO   = new ConfigurationIO();
        loadConfigurations();
    }

    public static ConfigurationManager getInstance() {
        return instance;
    }

    // -------------------------------------------------------------------------
    // Cache operations
    // -------------------------------------------------------------------------

    public void addConfiguration(Configuration configuration) {
        configurationCache.put(configuration.getName(), configuration);
    }

    public Configuration getConfiguration(String name) {
        return configurationCache.get(name);
    }

    public List<Configuration> getAllConfigurations() {
        return new ArrayList<>(configurationCache.values());
    }

    public boolean configurationExists(String name) {
        return configurationCache.containsKey(name);
    }

    public void removeConfiguration(String name) {
        configurationCache.remove(name);
    }

    public void updateConfiguration(String oldName, Configuration newConfiguration) {
        configurationCache.remove(oldName);
        configurationCache.put(newConfiguration.getName(), newConfiguration);
    }


    void clearCache() {
        configurationCache.clear();
    }

    // -------------------------------------------------------------------------
    // Persistence – load
    // -------------------------------------------------------------------------


    public void loadConfigurations() {
        configurationCache.clear();
        try {
            List<Configuration> configs = configurationIO.loadAllConfigurations();
            for (Configuration config : configs) {
                configurationCache.put(config.getName(), config);
            }
            System.out.println("Loaded " + configs.size() + " configuration(s) from disk.");
        } catch (Exception e) {
            System.err.println("Failed to load configurations: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Persistence – save
    // -------------------------------------------------------------------------


    public void saveConfiguration(Configuration configuration) throws ConfigurationPersistenceException {
        configurationCache.put(configuration.getName(), configuration);
        try {
            configurationIO.saveConfiguration(configuration);
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(
                    "Failed to save configuration '" + configuration.getName() + "'.", e);
        }
    }


    public void renameAndSaveConfiguration(String oldName, Configuration newConfiguration)
            throws ConfigurationPersistenceException {

        configurationCache.remove(oldName);
        configurationCache.put(newConfiguration.getName(), newConfiguration);

        try {
            configurationIO.saveConfiguration(newConfiguration);
            if (oldName != null && !oldName.equals(newConfiguration.getName())) {
                configurationIO.deleteFile(oldName);
            }
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(
                    "Failed to rename configuration from '" + oldName + "' to '"
                            + newConfiguration.getName() + "'.", e);
        }
    }


    public void saveAllConfigurations() throws ConfigurationPersistenceException {
        List<String> failures = new ArrayList<>();
        Throwable lastCause = null;

        for (Configuration config : configurationCache.values()) {
            try {
                configurationIO.saveConfiguration(config);
            } catch (Exception e) {
                System.err.println("Failed to save configuration '" + config.getName() + "': " + e.getMessage());
                failures.add(config.getName());
                lastCause = e;
            }
        }

        if (!failures.isEmpty()) {
            throw new ConfigurationPersistenceException(
                    "Failed to save the following configuration(s): " + failures, lastCause);
        }
    }

    // -------------------------------------------------------------------------
    // Persistence – delete
    // -------------------------------------------------------------------------


    public boolean deleteConfigurationFromDisk(String name) throws ConfigurationPersistenceException {
        configurationCache.remove(name);
        try {
            return configurationIO.deleteFile(name);
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(
                    "Failed to delete configuration file for '" + name + "'.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Import / Export (delegated to ConfigurationIO)
    // -------------------------------------------------------------------------

    public void exportConfiguration(String name, String filePath) throws ConfigurationPersistenceException {
        Configuration config = configurationCache.get(name);
        if (config == null) {
            throw new IllegalArgumentException("Configuration not found: " + name);
        }
        try {
            configurationIO.exportToFile(config, filePath);
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(
                    "Failed to export configuration '" + name + "' to '" + filePath + "'.", e);
        }
    }

    public Configuration importConfiguration(String filePath) throws ConfigurationPersistenceException {
        try {
            Configuration config = configurationIO.importFromFile(filePath);
            configurationCache.put(config.getName(), config);
            return config;
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(
                    "Failed to import configuration from '" + filePath + "'.", e);
        }
    }
}
