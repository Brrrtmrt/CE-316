package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.persistence.ConfigurationIO;

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


    public void loadConfigurations() {
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


    public void saveConfiguration(Configuration configuration) throws Exception {
        configurationCache.put(configuration.getName(), configuration);
        configurationIO.saveConfiguration(configuration);
    }


    public void renameAndSaveConfiguration(String oldName, Configuration newConfiguration) throws Exception {
        configurationCache.remove(oldName);
        configurationCache.put(newConfiguration.getName(), newConfiguration);

        configurationIO.saveConfiguration(newConfiguration);

        if (oldName != null && !oldName.equals(newConfiguration.getName())) {
            configurationIO.deleteFile(oldName);
        }
    }


    public void saveAllConfigurations() throws Exception {
        Exception lastError = null;
        for (Configuration config : configurationCache.values()) {
            try {
                configurationIO.saveConfiguration(config);
            } catch (Exception e) {
                System.err.println("Failed to save configuration '" + config.getName() + "': " + e.getMessage());
                lastError = e;
            }
        }
        if (lastError != null) {
            throw new Exception("One or more configurations could not be saved.", lastError);
        }
    }

    public boolean deleteConfigurationFromDisk(String name) throws Exception {
        configurationCache.remove(name);
        return configurationIO.deleteFile(name);
    }



    public void exportConfiguration(String name, String filePath) throws Exception {
        Configuration config = configurationCache.get(name);
        if (config == null) {
            throw new IllegalArgumentException("Configuration not found: " + name);
        }
        configurationIO.exportToFile(config, filePath);
    }

    public Configuration importConfiguration(String filePath) throws Exception {
        Configuration config = configurationIO.importFromFile(filePath);
        configurationCache.put(config.getName(), config);
        return config;
    }
}
