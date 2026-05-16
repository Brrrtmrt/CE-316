package com.iae.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.evaluation.strategies.ComparisonStrategy;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.evaluation.strategies.IgnoreWhitespaceStrategy;
import com.iae.evaluation.strategies.TrimLinesStrategy;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;


public class ConfigurationIO {

    private static final String CONFIG_DIR = "config";
    private static final String FILE_EXTENSION = ".json";

    private final Gson gson;

    public ConfigurationIO() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureConfigDirExists();
    }


    public void saveConfiguration(Configuration config) throws IOException {
        rejectIfNameWouldCollideOnDisk(config.getName());

        String fileName = sanitizeFileName(config.getName()) + FILE_EXTENSION;
        Path filePath = Paths.get(CONFIG_DIR, fileName);

        String json = gson.toJson(toJsonObject(config));

        try (Writer writer = new FileWriter(filePath.toFile())) {
            writer.write(json);
        }
    }


    public List<Configuration> loadAllConfigurations() throws IOException {
        List<Configuration> configurations = new ArrayList<>();
        Path configDir = Paths.get(CONFIG_DIR);

        if (!Files.exists(configDir)) {
            return configurations;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDir, "*" + FILE_EXTENSION)) {
            for (Path entry : stream) {
                try {
                    Configuration config = parseFromFile(entry.toString());
                    configurations.add(config);
                } catch (Exception e) {
                    System.err.println("Skipping invalid config file: " + entry.getFileName() + " — " + e.getMessage());
                }
            }
        }

        return configurations;
    }


    public Configuration importFromFile(String filePath) throws IOException {
        return parseFromFile(filePath);
    }


    public void exportToFile(Configuration config, String filePath) throws IOException {
        String json = gson.toJson(toJsonObject(config));

        try (Writer writer = new FileWriter(filePath)) {
            writer.write(json);
        }
    }


    /**
     * Deletes the JSON file backing the configuration with the given name.
     *
     * <p>Rejects names that would not survive sanitization unchanged, so two
     * distinct logical names cannot accidentally resolve to the same file.
     *
     * @param name the configuration's name
     * @return {@code true} if the file existed and was deleted, {@code false} if
     *         no matching file was found on disk
     * @throws IOException              if an I/O error occurs while deleting the file
     * @throws IllegalArgumentException if the name contains characters that would
     *                                  collide with another name after sanitization
     */
    public boolean deleteFile(String name) throws IOException {
        if (name == null || name.isBlank()) {
            return false;
        }
        rejectIfNameWouldCollideOnDisk(name);

        String fileName = sanitizeFileName(name) + FILE_EXTENSION;
        Path filePath = Paths.get(CONFIG_DIR, fileName);
        return Files.deleteIfExists(filePath);
    }


    private Configuration parseFromFile(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        JsonObject json = JsonParser.parseString(content).getAsJsonObject();

        String name            = getStringOrDefault(json, "name", "");
        String language        = getStringOrDefault(json, "language", "");
        String fileExtension   = getStringOrDefault(json, "fileExtension", "");
        String compileCommand  = getStringOrDefault(json, "compileCommand", "");
        String runCommand      = getStringOrDefault(json, "runCommand", "");
        String comparisonMethod = getStringOrDefault(json, "comparisonMethod", "exact");
        String description     = getStringOrDefault(json, "description", "");

        ComparisonStrategy strategy = resolveStrategy(comparisonMethod);

        return new ConfigurationBuilder()
                .setName(name)
                .setLanguage(language)
                .setFileExtension(fileExtension)
                .setCompileCommand(compileCommand)
                .setRunCommand(runCommand)
                .setComparisonStrategy(strategy)
                .setDescription(description)
                .build();
    }


    private JsonObject toJsonObject(Configuration config) {
        JsonObject json = new JsonObject();
        json.addProperty("name",             config.getName());
        json.addProperty("language",         config.getLanguage());
        json.addProperty("fileExtension",    config.getFileExtension());
        json.addProperty("compileCommand",   config.getCompileCommand());
        json.addProperty("runCommand",       config.getRunCommand());
        json.addProperty("comparisonMethod", strategyToKey(config.getComparisonStrategy()));
        json.addProperty("description",      config.getDescription());
        return json;
    }


    private ComparisonStrategy resolveStrategy(String method) {
        if (method == null) return new ExactMatchStrategy();
        return switch (method.toLowerCase()) {
            case "ignore_whitespace" -> new IgnoreWhitespaceStrategy();
            case "trim_lines"        -> new TrimLinesStrategy();
            default                  -> new ExactMatchStrategy(); // "exact" or unknown
        };
    }


    private String strategyToKey(ComparisonStrategy strategy) {
        if (strategy instanceof IgnoreWhitespaceStrategy) return "ignore_whitespace";
        if (strategy instanceof TrimLinesStrategy)        return "trim_lines";
        return "exact";
    }


    private String getStringOrDefault(JsonObject json, String key, String defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return defaultValue;
    }


    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
    }


    /**
     * Rejects names that contain characters which would be lost (replaced with {@code _})
     * during sanitization. This prevents two distinct logical names (e.g. {@code "My/Cfg"}
     * and {@code "My_Cfg"}) from resolving to the same JSON file on disk, which would
     * lead to silent data overwrites or accidental deletions.
     */
    private void rejectIfNameWouldCollideOnDisk(String name) {
        if (name == null) return;
        if (!sanitizeFileName(name).equals(name)) {
            throw new IllegalArgumentException(
                    "Configuration name contains characters that are not allowed on disk: '"
                            + name + "'. Allowed: letters, digits, dot, underscore, hyphen, space.");
        }
    }


    private void ensureConfigDirExists() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
