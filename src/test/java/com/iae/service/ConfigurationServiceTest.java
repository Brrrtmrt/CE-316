package com.iae.service;

import com.iae.domain.Configuration;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.evaluation.strategies.IgnoreWhitespaceStrategy;
import com.iae.evaluation.strategies.TrimLinesStrategy;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit / integration tests for {@link ConfigurationService}.
 *
 * <p>The singleton cache is cleared before each test via
 * {@link ConfigurationManager#clearCache()}, so tests do not influence each other.
 *
 * <p>Any {@code config/Test_*.json} files created during a test are removed
 * afterwards to keep the working directory clean.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigurationServiceTest {

    private static final Path CONFIG_DIR = Paths.get("config");
    private ConfigurationService service;

    @BeforeEach
    void setUp() {
        ConfigurationManager.getInstance().clearCache();
        service = new ConfigurationService();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(CONFIG_DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(CONFIG_DIR, "Test_*.json")) {
                for (Path entry : stream) {
                    Files.deleteIfExists(entry);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("createConfiguration — creates and caches successfully")
    void testCreate_success() throws Exception {
        Configuration config = service.createConfiguration(
                "Test_Java17", "Java", ".java",
                "javac {src}", "java Main",
                new ExactMatchStrategy(), "Java 17 config"
        );

        assertNotNull(config);
        assertEquals("Test_Java17", config.getName());
        assertEquals("Java", config.getLanguage());
        assertTrue(service.exists("Test_Java17"));
    }

    @Test
    @Order(2)
    @DisplayName("createConfiguration — writes a JSON file to disk")
    void testCreate_writesJsonFile() throws Exception {
        service.createConfiguration(
                "Test_FileCheck", "Python", ".py",
                "", "python3 {src}",
                new ExactMatchStrategy(), ""
        );

        File jsonFile = new File("config", "Test_FileCheck.json");
        assertTrue(jsonFile.exists(), "JSON file should be created under config/");
    }

    @Test
    @Order(3)
    @DisplayName("createConfiguration — duplicate name throws exception")
    void testCreate_duplicateName_throwsException() throws Exception {
        service.createConfiguration("Test_Dup", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");

        assertThrows(IllegalArgumentException.class, () ->
                service.createConfiguration("Test_Dup", "C++", ".cpp", "g++ {src}", "./{out}",
                        new ExactMatchStrategy(), "")
        );
    }

    @Test
    @Order(4)
    @DisplayName("createConfiguration — blank name throws exception")
    void testCreate_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createConfiguration("", "Java", ".java", "", "java Main",
                        new ExactMatchStrategy(), "")
        );
    }

    @Test
    @Order(5)
    @DisplayName("createConfiguration — blank runCommand throws exception")
    void testCreate_blankRunCommand_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createConfiguration("Test_NoRun", "Java", ".java", "javac {src}", "",
                        new ExactMatchStrategy(), "")
        );
    }

    @Test
    @Order(6)
    @DisplayName("createConfiguration — null ComparisonStrategy throws exception")
    void testCreate_nullStrategy_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createConfiguration("Test_NullStrat", "Java", ".java",
                        "javac {src}", "java Main", null, "")
        );
    }

    @Test
    @Order(7)
    @DisplayName("createConfiguration — name with filesystem-unsafe characters is rejected")
    void testCreate_unsafeName_throwsException() {
        // "/" is a path separator and would be replaced by "_" during sanitization,
        // creating a collision risk with another logically distinct name.
        assertThrows(IllegalArgumentException.class, () ->
                        service.createConfiguration("Test_Bad/Name", "Java", ".java",
                                "javac {src}", "java Main", new ExactMatchStrategy(), ""),
                "Names containing '/' must be rejected to avoid silent overwrites"
        );
    }

    @Test
    @Order(8)
    @DisplayName("createConfiguration — failed save does not pollute the cache")
    void testCreate_failedSave_doesNotPolluteCache() {
        // An unsafe name causes the IO layer to throw IllegalArgumentException.
        // Verify the cache is not mutated as a side effect.
        assertThrows(Exception.class, () ->
                service.createConfiguration("Bad/Name", "Java", ".java",
                        "javac {src}", "java Main", new ExactMatchStrategy(), "")
        );

        assertFalse(service.exists("Bad/Name"),
                "Cache must not contain a ghost entry when the disk write failed");
    }

    // -----------------------------------------------------------------------
    // READ
    // -----------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("getConfiguration — returns existing config")
    void testGet_existingConfig() throws Exception {
        service.createConfiguration("Test_Get", "Python", ".py", "", "python3 {src}",
                new ExactMatchStrategy(), "desc");

        Configuration found = service.getConfiguration("Test_Get");
        assertNotNull(found);
        assertEquals("Python", found.getLanguage());
    }

    @Test
    @Order(10)
    @DisplayName("getConfiguration — returns null for missing name")
    void testGet_nonExisting_returnsNull() {
        assertNull(service.getConfiguration("NoSuchConfigExists"));
    }

    @Test
    @Order(11)
    @DisplayName("getAllConfigurations — lists every added config")
    void testGetAll_returnsAll() throws Exception {
        service.createConfiguration("Test_All1", "Java", ".java", "javac {src}", "java Main",
                new ExactMatchStrategy(), "");
        service.createConfiguration("Test_All2", "Python", ".py", "", "python3 {src}",
                new ExactMatchStrategy(), "");

        List<Configuration> all = service.getAllConfigurations();
        long count = all.stream().filter(c -> c.getName().startsWith("Test_All")).count();
        assertEquals(2, count);
    }

    @Test
    @Order(12)
    @DisplayName("exists — returns the correct boolean")
    void testExists() throws Exception {
        assertFalse(service.exists("Test_Var"));
        service.createConfiguration("Test_Var", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");
        assertTrue(service.exists("Test_Var"));
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    @Test
    @Order(13)
    @DisplayName("updateConfiguration — fields are updated")
    void testUpdate_fieldsChanged() throws Exception {
        service.createConfiguration("Test_Upd", "Java", ".java", "javac {src}", "java Main",
                new ExactMatchStrategy(), "old");

        service.updateConfiguration(
                "Test_Upd", "Test_Upd",
                "Java 17", ".java",
                "javac -source 17 {src}", "java Main",
                new TrimLinesStrategy(), "new description"
        );

        Configuration updated = service.getConfiguration("Test_Upd");
        assertEquals("Java 17", updated.getLanguage());
        assertEquals("new description", updated.getDescription());
        assertInstanceOf(TrimLinesStrategy.class, updated.getComparisonStrategy());
    }

    @Test
    @Order(14)
    @DisplayName("updateConfiguration — rename updates the cache")
    void testUpdate_renameConfig_updatesCache() throws Exception {
        service.createConfiguration("Test_OldName", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");

        service.updateConfiguration("Test_OldName", "Test_NewName", "C", ".c",
                "gcc {src}", "./{out}", new ExactMatchStrategy(), "");

        assertFalse(service.exists("Test_OldName"), "Old name should no longer exist");
        assertTrue(service.exists("Test_NewName"), "New name should be present in the cache");
    }

    @Test
    @Order(15)
    @DisplayName("updateConfiguration — old JSON file is deleted on rename (no orphan)")
    void testUpdate_renameConfig_oldFileDeletedFromDisk() throws Exception {
        service.createConfiguration("Test_Orphan", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");

        File oldFile = new File("config", "Test_Orphan.json");
        File newFile = new File("config", "Test_Renamed.json");
        assertTrue(oldFile.exists(), "Precondition: old file should exist on disk");

        service.updateConfiguration("Test_Orphan", "Test_Renamed", "C", ".c",
                "gcc {src}", "./{out}", new ExactMatchStrategy(), "");

        assertFalse(oldFile.exists(), "Old file must be deleted to avoid an orphan");
        assertTrue(newFile.exists(), "Renamed file should exist on disk");
    }

    @Test
    @Order(16)
    @DisplayName("updateConfiguration — updating a missing config throws exception")
    void testUpdate_nonExisting_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.updateConfiguration("Test_Ghost", "Test_Ghost", "C", ".c",
                        "gcc {src}", "./{out}", new ExactMatchStrategy(), "")
        );
    }

    @Test
    @Order(17)
    @DisplayName("updateConfiguration — null ComparisonStrategy throws exception")
    void testUpdate_nullStrategy_throwsException() throws Exception {
        service.createConfiguration("Test_UpdNullStrat", "Java", ".java",
                "javac {src}", "java Main", new ExactMatchStrategy(), "");

        assertThrows(IllegalArgumentException.class, () ->
                service.updateConfiguration("Test_UpdNullStrat", "Test_UpdNullStrat",
                        "Java", ".java", "javac {src}", "java Main", null, "")
        );
    }

    // -----------------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------------

    @Test
    @Order(18)
    @DisplayName("deleteConfiguration — removes config from the cache")
    void testDelete_removesFromCache() throws Exception {
        service.createConfiguration("Test_Del", "Python", ".py", "", "python3 {src}",
                new ExactMatchStrategy(), "");
        assertTrue(service.exists("Test_Del"));

        service.deleteConfiguration("Test_Del");

        assertFalse(service.exists("Test_Del"), "Deleted config must no longer be in the cache");
    }

    @Test
    @Order(19)
    @DisplayName("deleteConfiguration — removes JSON file from disk (hard delete)")
    void testDelete_removesFileFromDisk() throws Exception {
        service.createConfiguration("Test_HardDel", "Python", ".py", "", "python3 {src}",
                new ExactMatchStrategy(), "");

        File jsonFile = new File("config", "Test_HardDel.json");
        assertTrue(jsonFile.exists(), "Precondition: file should exist on disk");

        service.deleteConfiguration("Test_HardDel");

        assertFalse(jsonFile.exists(), "File should be deleted from disk");
    }

    @Test
    @Order(20)
    @DisplayName("deleteConfiguration — deleted config does not reappear after reload")
    void testDelete_doesNotReappearAfterReload() throws Exception {
        service.createConfiguration("Test_NoReappear", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");

        service.deleteConfiguration("Test_NoReappear");
        service.reloadFromDisk();

        assertFalse(service.exists("Test_NoReappear"),
                "Deleted config must not come back after a disk reload");
    }

    @Test
    @Order(21)
    @DisplayName("deleteConfiguration — deleting a missing config throws exception")
    void testDelete_nonExisting_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.deleteConfiguration("Test_NotHere")
        );
    }

    // -----------------------------------------------------------------------
    // RELOAD
    // -----------------------------------------------------------------------

    @Test
    @Order(22)
    @DisplayName("reloadFromDisk — unsaved cache entries are removed")
    void testReload_clearsUnsavedCacheEntries() {
        Configuration ghost = new com.iae.domain.ConfigurationBuilder()
                .setName("Test_GhostInCache")
                .setLanguage("Java")
                .setFileExtension(".java")
                .setCompileCommand("javac {src}")
                .setRunCommand("java Main")
                .setComparisonStrategy(new ExactMatchStrategy())
                .setDescription("")
                .build();
        ConfigurationManager.getInstance().addConfiguration(ghost);
        assertTrue(service.exists("Test_GhostInCache"), "Precondition: entry should be in the cache");

        service.reloadFromDisk();

        assertFalse(service.exists("Test_GhostInCache"),
                "Cache entries that are not on disk must be evicted after reload");
    }

    // -----------------------------------------------------------------------
    // ComparisonStrategy round-trip
    // -----------------------------------------------------------------------

    @Test
    @Order(23)
    @DisplayName("IgnoreWhitespaceStrategy is preserved on creation")
    void testCreate_ignoreWhitespaceStrategy_preserved() throws Exception {
        service.createConfiguration(
                "Test_IWS", "Python", ".py",
                "", "python3 {src}",
                new IgnoreWhitespaceStrategy(), ""
        );

        Configuration found = service.getConfiguration("Test_IWS");
        assertInstanceOf(IgnoreWhitespaceStrategy.class, found.getComparisonStrategy());
    }
}