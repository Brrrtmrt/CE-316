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
        this.configurationIO = new ConfigurationIO();
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

    public void removeConfiguration(String name) {
        configurationCache.remove(name);
    }


    public void updateConfiguration(String oldName, Configuration newConfiguration) {
        configurationCache.remove(oldName);
        configurationCache.put(newConfiguration.getName(), newConfiguration);
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

    private void loadConfigurations() {
        try {
            List<Configuration> configs = configurationIO.loadAllConfigurations();
            for (Configuration config : configs) {
                configurationCache.put(config.getName(), config);
            }
        } catch (Exception e) {
            // Log error,start with empty config.
            System.err.println("Failed to load configurations: " + e.getMessage());
        }
    }

    public void saveAllConfigurations() throws Exception {
        for (Configuration config : configurationCache.values()) {
            configurationIO.saveConfiguration(config);
        }
    }
}
