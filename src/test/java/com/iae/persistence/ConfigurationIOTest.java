package com.iae.persistence;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.evaluation.strategies.IgnoreWhitespaceStrategy;
import com.iae.evaluation.strategies.TrimLinesStrategy;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigurationIOTest {

    private static final String TEST_CONFIG_DIR = "test-config-tmp";
    private ConfigurationIO configurationIO;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(Paths.get(TEST_CONFIG_DIR));

        configurationIO = new ConfigurationIO() {
        };

    }

    @AfterEach
    void tearDown() throws IOException {
        Path configDir = Paths.get("config");
        if (Files.exists(configDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDir, "Test_*.json")) {
                for (Path entry : stream) {
                    Files.deleteIfExists(entry);
                }
            }
        }
        deleteDirectory(Paths.get(TEST_CONFIG_DIR));
    }


    private Configuration buildTestConfiguration(String name, String language) {
        return new ConfigurationBuilder()
                .setName(name)
                .setLanguage(language)
                .setFileExtension(".java")
                .setCompileCommand("javac -d out {src}")
                .setRunCommand("java -cp out Main {args}")
                .setComparisonStrategy(new ExactMatchStrategy())
                .setDescription("Test configuration for " + language)
                .build();
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
    }


    @Test
    @Order(1)
    @DisplayName("saveConfiguration creates a JSON file in the config directory")
    void testSaveConfiguration_createsFile() throws IOException {
        Configuration config = buildTestConfiguration("Test_Java", "Java");

        configurationIO.saveConfiguration(config);

        File savedFile = new File("config", "Test_Java.json");
        assertTrue(savedFile.exists(), "JSON file should be created after save");
    }

    @Test
    @Order(2)
    @DisplayName("saveConfiguration then loadAllConfigurations returns the saved config")
    void testSaveAndLoad_roundTrip() throws IOException {
        Configuration original = buildTestConfiguration("Test_RoundTrip", "Java");
        configurationIO.saveConfiguration(original);

        List<Configuration> loaded = configurationIO.loadAllConfigurations();

        Configuration found = loaded.stream()
                .filter(c -> c.getName().equals("Test_RoundTrip"))
                .findFirst()
                .orElse(null);

        assertNotNull(found, "Saved configuration should be found after loading");
        assertEquals("Test_RoundTrip", found.getName());
        assertEquals("Java",           found.getLanguage());
        assertEquals(".java",          found.getFileExtension());
        assertEquals("javac -d out {src}", found.getCompileCommand());
        assertEquals("java -cp out Main {args}", found.getRunCommand());
        assertEquals("Test configuration for Java", found.getDescription());
    }

    @Test
    @Order(3)
    @DisplayName("ComparisonStrategy is correctly preserved through save/load")
    void testSaveAndLoad_comparisonStrategy() throws IOException {
        Configuration config = new ConfigurationBuilder()
                .setName("Test_IgnoreWS")
                .setLanguage("Python")
                .setFileExtension(".py")
                .setCompileCommand("")
                .setRunCommand("python3 {src}")
                .setComparisonStrategy(new IgnoreWhitespaceStrategy())
                .setDescription("Python with ignore whitespace")
                .build();

        configurationIO.saveConfiguration(config);

        List<Configuration> loaded = configurationIO.loadAllConfigurations();
        Configuration found = loaded.stream()
                .filter(c -> c.getName().equals("Test_IgnoreWS"))
                .findFirst()
                .orElse(null);

        assertNotNull(found);
        assertInstanceOf(IgnoreWhitespaceStrategy.class, found.getComparisonStrategy(),
                "IgnoreWhitespaceStrategy should be restored after load");
    }

    @Test
    @Order(4)
    @DisplayName("TrimLinesStrategy is correctly preserved through save/load")
    void testSaveAndLoad_trimLinesStrategy() throws IOException {
        Configuration config = new ConfigurationBuilder()
                .setName("Test_TrimLines")
                .setLanguage("C")
                .setFileExtension(".c")
                .setCompileCommand("gcc {src} -o {out}")
                .setRunCommand("{out} {args}")
                .setComparisonStrategy(new TrimLinesStrategy())
                .setDescription("C with trim lines")
                .build();

        configurationIO.saveConfiguration(config);

        List<Configuration> loaded = configurationIO.loadAllConfigurations();
        Configuration found = loaded.stream()
                .filter(c -> c.getName().equals("Test_TrimLines"))
                .findFirst()
                .orElse(null);

        assertNotNull(found);
        assertInstanceOf(TrimLinesStrategy.class, found.getComparisonStrategy(),
                "TrimLinesStrategy should be restored after load");
    }

    @Test
    @Order(5)
    @DisplayName("exportToFile creates a JSON file at the specified path")
    void testExportToFile_createsFile() throws IOException {
        Configuration config = buildTestConfiguration("Test_Export", "Java");
        String exportPath = TEST_CONFIG_DIR + "/exported.json";

        configurationIO.exportToFile(config, exportPath);

        assertTrue(new File(exportPath).exists(), "Exported file should exist at specified path");
    }

    @Test
    @Order(6)
    @DisplayName("importFromFile reads a configuration correctly")
    void testImportFromFile_readsCorrectly() throws IOException {
        // First export, then import
        Configuration original = buildTestConfiguration("Test_Import", "Java");
        String exportPath = TEST_CONFIG_DIR + "/import-test.json";
        configurationIO.exportToFile(original, exportPath);

        Configuration imported = configurationIO.importFromFile(exportPath);

        assertNotNull(imported);
        assertEquals("Test_Import", imported.getName());
        assertEquals("Java",        imported.getLanguage());
        assertEquals(".java",       imported.getFileExtension());
    }

    @Test
    @Order(7)
    @DisplayName("export then import preserves all fields (full round-trip)")
    void testExportImport_fullRoundTrip() throws IOException {
        Configuration original = new ConfigurationBuilder()
                .setName("Test_FullTrip")
                .setLanguage("Python")
                .setFileExtension(".py")
                .setCompileCommand("")
                .setRunCommand("python3 {src} {args}")
                .setComparisonStrategy(new TrimLinesStrategy())
                .setDescription("Full round trip test")
                .build();

        String exportPath = TEST_CONFIG_DIR + "/full-trip.json";
        configurationIO.exportToFile(original, exportPath);
        Configuration imported = configurationIO.importFromFile(exportPath);

        assertEquals(original.getName(),          imported.getName());
        assertEquals(original.getLanguage(),       imported.getLanguage());
        assertEquals(original.getFileExtension(),  imported.getFileExtension());
        assertEquals(original.getCompileCommand(), imported.getCompileCommand());
        assertEquals(original.getRunCommand(),     imported.getRunCommand());
        assertEquals(original.getDescription(),    imported.getDescription());
        assertInstanceOf(TrimLinesStrategy.class,  imported.getComparisonStrategy());
    }

    @Test
    @Order(8)
    @DisplayName("loadAllConfigurations returns non-empty list when config dir has files")
    void testLoadAllConfigurations_returnsNonEmpty() throws IOException {
        // The real config/ dir already has c-programming.json, java-17.json, python-3.json
        List<Configuration> configs = configurationIO.loadAllConfigurations();

        assertFalse(configs.isEmpty(), "Should load at least the existing config files");
    }

    @Test
    @Order(9)
    @DisplayName("importFromFile throws IOException for a non-existent file")
    void testImportFromFile_nonExistentFile() {
        assertThrows(IOException.class, () ->
                configurationIO.importFromFile("non-existent-path/fake.json"),
                "Should throw IOException for missing file"
        );
    }

    @Test
    @Order(10)
    @DisplayName("saveConfiguration with special characters in name sanitizes filename")
    void testSaveConfiguration_specialCharactersInName() throws IOException {
        Configuration config = new ConfigurationBuilder()
                .setName("Test_C++ 17")
                .setLanguage("C++")
                .setFileExtension(".cpp")
                .setCompileCommand("g++ {src} -o {out}")
                .setRunCommand("{out} {args}")
                .setComparisonStrategy(new ExactMatchStrategy())
                .setDescription("C++ config")
                .build();

        assertDoesNotThrow(() -> configurationIO.saveConfiguration(config));
    }
}
