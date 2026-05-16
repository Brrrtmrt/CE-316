package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.persistence.ConfigurationIO;
import com.iae.persistence.ConfigurationPersistenceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton service that manages the in-memory configuration cache
 * and delegates persistence to {@link ConfigurationIO}.
 *
 * <p>Sprint 2 additions:
 * <ul>
 *   <li>{@link #saveConfiguration(Configuration)} – persist a single config immediately.</li>
 *   <li>{@link #renameAndSaveConfiguration(String, Configuration)} – rename-safe save (no orphan files).</li>
 *   <li>{@link #saveAllConfigurations()} – persist every cached config at once.</li>
 *   <li>{@link #loadConfigurations()} – called at startup; rebuilds the cache from disk.</li>
 *   <li>{@link #deleteConfigurationFromDisk(String)} – hard-delete (cache + file).</li>
 *   <li>{@link #clearCache()} – package-private; intended for unit tests only.</li>
 * </ul>
 *
 * <p><strong>Consistency contract:</strong> all mutating persistence operations
 * follow a "disk-first, cache-on-success" pattern. If the disk write fails, the
 * in-memory cache is left untouched so the manager never reports a configuration
 * that does not actually exist on disk.
 */
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

    /**
     * Empties the in-memory cache without touching disk.
     *
     * <p><strong>Package-private on purpose:</strong> this method is intended for
     * unit tests that need a clean slate between cases. Production code should
     * use {@link #removeConfiguration(String)} or
     * {@link #deleteConfigurationFromDisk(String)} instead.
     */
    void clearCache() {
        configurationCache.clear();
    }

    // -------------------------------------------------------------------------
    // Persistence – load
    // -------------------------------------------------------------------------

    /**
     * Rebuilds the in-memory cache from the {@code config/} directory.
     *
     * <p>The cache is cleared first, so this method is a true "reload from disk":
     * configurations that exist only in memory (e.g. unsaved additions) or that
     * were removed externally from disk will no longer be present after the call.
     *
     * <p><strong>Note (destructive on failure):</strong> if {@link ConfigurationIO}
     * cannot read from disk, the cache is left empty and the error is logged
     * to {@code System.err}. Callers that need stronger guarantees should
     * snapshot {@link #getAllConfigurations()} before invoking reload.
     */
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
    // Persistence – save (disk-first, cache-on-success)
    // -------------------------------------------------------------------------

    /**
     * Persists a single configuration to disk and, on success, updates the cache.
     * If the disk write fails, the cache is left untouched.
     *
     * @param configuration the configuration to save
     * @throws ConfigurationPersistenceException if the file cannot be written
     */
    public void saveConfiguration(Configuration configuration) throws ConfigurationPersistenceException {
        try {
            configurationIO.saveConfiguration(configuration);
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(
                    "Failed to save configuration '" + configuration.getName() + "'.", e);
        }
        // Disk write succeeded — now safe to mutate the cache.
        configurationCache.put(configuration.getName(), configuration);
    }

    /**
     * Renames a configuration on disk: writes the new file and deletes the
     * old one to avoid orphan JSON files. On success, the cache is updated.
     *
     * <p>If {@code oldName} equals the new configuration's name (i.e. no rename),
     * this method behaves identically to {@link #saveConfiguration(Configuration)}.
     *
     * <p>If the disk operations fail, both the cache and the previous
     * configuration mapping for {@code oldName} are preserved.
     *
     * @param oldName          the previous name of the configuration
     * @param newConfiguration the configuration with the new name and possibly other changes
     * @throws ConfigurationPersistenceException if the new file cannot be written
     *                                           or the old file cannot be deleted
     */
    public void renameAndSaveConfiguration(String oldName, Configuration newConfiguration)
            throws ConfigurationPersistenceException {

        try {
            // 1) Write the new file
            configurationIO.saveConfiguration(newConfiguration);
            // 2) Delete the old file IFF the name actually changed
            if (oldName != null && !oldName.equals(newConfiguration.getName())) {
                configurationIO.deleteFile(oldName);
            }
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(
                    "Failed to rename configuration from '" + oldName + "' to '"
                            + newConfiguration.getName() + "'.", e);
        }

        // Disk operations succeeded — now safe to mutate the cache.
        configurationCache.remove(oldName);
        configurationCache.put(newConfiguration.getName(), newConfiguration);
    }

    /**
     * Persists every configuration currently in the cache to disk.
     * Useful for a "save all" action or application shutdown.
     *
     * <p>If one configuration fails to save, the method continues with the rest
     * and aggregates the failed names into a single exception at the end.
     *
     * @throws ConfigurationPersistenceException if at least one save operation failed
     */
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
    // Persistence – delete (disk-first, cache-on-success)
    // -------------------------------------------------------------------------

    /**
     * Hard-delete: deletes the JSON file from disk and, on success, removes the
     * entry from the cache. If the disk delete fails, the cache is left untouched
     * so the manager keeps reflecting the actual on-disk state.
     *
     * @param name the configuration name to delete
     * @return {@code true} if a file was deleted on disk, {@code false} if no
     *         backing file existed (cache is still cleared in that case because
     *         the desired end-state — "name not present" — is already true)
     * @throws ConfigurationPersistenceException if an I/O error occurs while deleting the file
     */
    public boolean deleteConfigurationFromDisk(String name) throws ConfigurationPersistenceException {
        boolean fileExisted;
        try {
            fileExisted = configurationIO.deleteFile(name);
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(
                    "Failed to delete configuration file for '" + name + "'.", e);
        }
        // Disk delete succeeded — now safe to remove from cache.
        configurationCache.remove(name);
        return fileExisted;
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
        Configuration config;
        try {
            config = configurationIO.importFromFile(filePath);
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(
                    "Failed to import configuration from '" + filePath + "'.", e);
        }
        configurationCache.put(config.getName(), config);
        return config;
    }
}
