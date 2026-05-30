package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.persistence.ConfigurationPersistenceException;
import com.iae.util.Constants;
import com.iae.util.Logger;
import com.iae.util.Validator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * High-level service that handles importing and exporting {@link Configuration}
 * files, keeping the GUI controllers free of any persistence details.
 *
 * <p>It sits on top of {@link ConfigurationManager}: the manager owns the
 * in-memory cache and the low-level Gson read/write, while this service adds the
 * file-handling concerns that the user interface cares about:
 * <ul>
 *   <li>validating the source/destination paths and the imported configuration;</li>
 *   <li>making an imported configuration <em>durable</em> (it is written into the
 *       application's {@code config} directory, not merely cached in memory);</li>
 *   <li>resolving name clashes through a selectable {@link ConflictPolicy}; and</li>
 *   <li>normalising the export file name (ensuring a {@code .json} extension) and
 *       supporting a one-shot "export everything" operation.</li>
 * </ul>
 *
 * <p>All outcomes are reported through small immutable result objects so the
 * caller can give the user precise feedback (e.g. "renamed to avoid a clash").
 */
public class ImportExportService {

    /** How {@link #importConfiguration(String, ConflictPolicy)} reacts to a name clash. */
    public enum ConflictPolicy {
        /** Overwrite the existing configuration of the same name. */
        OVERWRITE,
        /** Import under an automatically generated, non-clashing name. */
        RENAME,
        /** Abort the import and leave the existing configuration untouched. */
        FAIL
    }

    /**
     * Outcome of a single import.
     *
     * @param configurationName the name the configuration was stored under
     * @param originalName       the name found inside the imported file
     * @param replacedExisting   {@code true} if an existing configuration was overwritten
     * @param renamed            {@code true} if the configuration was stored under a new name
     */
    public record ImportResult(String configurationName,
                               String originalName,
                               boolean replacedExisting,
                               boolean renamed) {
    }

    /**
     * Outcome of a bulk export.
     *
     * @param exportedCount how many configurations were written successfully
     * @param failedNames   the names of configurations that could not be written
     */
    public record ExportSummary(int exportedCount, List<String> failedNames) {
        public boolean hasFailures() {
            return failedNames != null && !failedNames.isEmpty();
        }
    }

    private final ConfigurationManager manager;

    /** Creates a service backed by the singleton {@link ConfigurationManager}. */
    public ImportExportService() {
        this(ConfigurationManager.getInstance());
    }

    /**
     * Package-private constructor that allows tests to inject a manager.
     *
     * @param manager the configuration manager to delegate to
     */
    ImportExportService(ConfigurationManager manager) {
        this.manager = manager;
    }

    // -------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------

    /**
     * Imports a configuration file, overwriting any existing configuration that
     * has the same name.
     *
     * @param sourcePath path to the {@code .json} file to import
     * @return a description of what happened
     * @throws ConfigurationPersistenceException if the file is missing or cannot be read/written
     * @throws IllegalArgumentException          if the file's contents are not a valid configuration
     */
    public ImportResult importConfiguration(String sourcePath) throws ConfigurationPersistenceException {
        return importConfiguration(sourcePath, ConflictPolicy.OVERWRITE);
    }

    /**
     * Imports a configuration file using the supplied conflict policy.
     *
     * <p>On success the configuration is persisted to the application's
     * configuration directory so that it survives a restart - this is the main
     * difference from caching it in memory only.
     *
     * @param sourcePath path to the {@code .json} file to import
     * @param policy     how to react if a configuration of the same name already exists
     * @return a description of what happened
     * @throws ConfigurationPersistenceException if the file is missing, cannot be read/written,
     *                                           or the policy is {@link ConflictPolicy#FAIL} and a clash occurs
     * @throws IllegalArgumentException          if the file's contents are not a valid configuration
     */
    public ImportResult importConfiguration(String sourcePath, ConflictPolicy policy)
            throws ConfigurationPersistenceException {

        Validator.requireNotBlank(sourcePath, "Source path");
        Validator.requireNotNull(policy, "Conflict policy");

        File source = new File(sourcePath);
        if (!source.isFile()) {
            throw new ConfigurationPersistenceException("Import file not found: " + sourcePath);
        }

        // Remember the names that exist before the import so we can detect a clash.
        Set<String> existingBefore = currentNames();

        // ConfigurationManager.importConfiguration reads the file and caches the
        // result, but does not write it to the config directory.
        Configuration imported = manager.importConfiguration(source.getAbsolutePath());
        Validator.requireValidConfiguration(imported);

        String name = imported.getName();
        boolean clash = existingBefore.contains(name);

        if (clash && policy != ConflictPolicy.OVERWRITE) {
            // The in-memory cache entry was just overwritten by importConfiguration.
            // Reload from disk to restore the original truth before we proceed.
            manager.loadConfigurations();

            if (policy == ConflictPolicy.FAIL) {
                Logger.warn("Import aborted: a configuration named '" + name + "' already exists.");
                throw new ConfigurationPersistenceException(
                        "A configuration named '" + name + "' already exists.");
            }

            // ConflictPolicy.RENAME
            String uniqueName = uniqueName(name, existingBefore);
            Configuration renamed = copyWithName(imported, uniqueName);
            manager.saveConfiguration(renamed);
            Logger.info("Imported configuration as '" + uniqueName
                    + "' (renamed to avoid a clash with '" + name + "').");
            return new ImportResult(uniqueName, name, false, true);
        }

        // No clash, or OVERWRITE policy: persist the imported configuration as-is.
        manager.saveConfiguration(imported);
        Logger.info("Imported configuration '" + name + "'"
                + (clash ? " (replaced the existing one)." : "."));
        return new ImportResult(name, name, clash, false);
    }

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    /**
     * Exports a single configuration to the given path. A {@code .json} extension
     * is appended automatically if the destination does not already have one.
     *
     * @param name            the name of the configuration to export
     * @param destinationPath the file path to write to
     * @return the absolute path that was actually written
     * @throws ConfigurationPersistenceException if writing fails
     * @throws IllegalArgumentException          if the configuration does not exist
     */
    public String exportConfiguration(String name, String destinationPath)
            throws ConfigurationPersistenceException {

        Validator.requireNotBlank(name, "Configuration name");
        Validator.requireNotBlank(destinationPath, "Destination path");

        String target = ensureJsonExtension(destinationPath);
        manager.exportConfiguration(name, target);
        Logger.info("Exported configuration '" + name + "' to " + target);
        return target;
    }

    /**
     * Exports every configuration currently known to the manager into the given
     * directory, one {@code .json} file per configuration. A failure on one
     * configuration does not stop the others.
     *
     * @param destinationDir the directory to write the files into (created if absent)
     * @return a summary listing how many succeeded and which ones failed
     * @throws ConfigurationPersistenceException if the directory cannot be created or is invalid
     */
    public ExportSummary exportAllConfigurations(String destinationDir)
            throws ConfigurationPersistenceException {

        Validator.requireNotBlank(destinationDir, "Destination directory");

        File dir = new File(destinationDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new ConfigurationPersistenceException(
                    "Could not create export directory: " + destinationDir);
        }
        if (!dir.isDirectory()) {
            throw new ConfigurationPersistenceException(
                    "Export target is not a directory: " + destinationDir);
        }

        List<Configuration> all = manager.getAllConfigurations();
        List<String> failed = new ArrayList<>();
        int exported = 0;

        for (Configuration config : all) {
            String fileName = safeFileName(config.getName()) + Constants.CONFIG_FILE_EXTENSION;
            File target = new File(dir, fileName);
            try {
                manager.exportConfiguration(config.getName(), target.getAbsolutePath());
                exported++;
            } catch (Exception e) {
                Logger.error("Failed to export configuration '" + config.getName() + "'", e);
                failed.add(config.getName());
            }
        }

        Logger.info("Exported " + exported + " of " + all.size()
                + " configuration(s) to " + destinationDir);
        return new ExportSummary(exported, failed);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Set<String> currentNames() {
        Set<String> names = new HashSet<>();
        for (Configuration config : manager.getAllConfigurations()) {
            names.add(config.getName());
        }
        return names;
    }

    /**
     * Builds a name that is not already taken, by appending {@code " (imported)"}
     * and then a counter if necessary.
     */
    private String uniqueName(String base, Set<String> taken) {
        String candidate = base + " (imported)";
        if (!taken.contains(candidate)) {
            return candidate;
        }
        int counter = 2;
        while (taken.contains(base + " (imported " + counter + ")")) {
            counter++;
        }
        return base + " (imported " + counter + ")";
    }

    /** Rebuilds a configuration identical to {@code source} but with a different name. */
    private Configuration copyWithName(Configuration source, String newName) {
        return new ConfigurationBuilder()
                .setName(newName)
                .setLanguage(source.getLanguage())
                .setFileExtension(source.getFileExtension())
                .setCompileCommand(source.getCompileCommand())
                .setRunCommand(source.getRunCommand())
                .setComparisonStrategy(source.getComparisonStrategy())
                .setDescription(source.getDescription())
                .build();
    }

    /** Ensures the path ends with {@code .json} (case-insensitive). */
    private String ensureJsonExtension(String path) {
        if (path.toLowerCase().endsWith(Constants.CONFIG_FILE_EXTENSION)) {
            return path;
        }
        return path + Constants.CONFIG_FILE_EXTENSION;
    }

    /** Replaces filesystem-unsafe characters and whitespace runs with underscores. */
    private String safeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_")
                   .replaceAll("\\s+", "_");
    }
}
