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
 * <p>Her testten önce {@link ConfigurationManager#clearCache()} ile singleton
 * temizleniyor; testler birbirini etkilemez.
 *
 * <p>Her testten sonra {@code config/Test_*.json} dosyaları da temizleniyor.
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
    @DisplayName("createConfiguration — başarılı oluşturma ve cache'e ekleme")
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
    @DisplayName("createConfiguration — disk'e JSON dosyası oluşturulur")
    void testCreate_writesJsonFile() throws Exception {
        service.createConfiguration(
                "Test_FileCheck", "Python", ".py",
                "", "python3 {src}",
                new ExactMatchStrategy(), ""
        );

        File jsonFile = new File("config", "Test_FileCheck.json");
        assertTrue(jsonFile.exists(), "JSON dosyası config/ altında oluşturulmalı");
    }

    @Test
    @Order(3)
    @DisplayName("createConfiguration — aynı isimde ikinci kayıt exception fırlatır")
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
    @DisplayName("createConfiguration — boş name ile exception fırlatır")
    void testCreate_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createConfiguration("", "Java", ".java", "", "java Main",
                        new ExactMatchStrategy(), "")
        );
    }

    @Test
    @Order(5)
    @DisplayName("createConfiguration — boş runCommand ile exception fırlatır")
    void testCreate_blankRunCommand_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createConfiguration("Test_NoRun", "Java", ".java", "javac {src}", "",
                        new ExactMatchStrategy(), "")
        );
    }

    @Test
    @Order(6)
    @DisplayName("createConfiguration — null ComparisonStrategy ile exception fırlatır")
    void testCreate_nullStrategy_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createConfiguration("Test_NullStrat", "Java", ".java",
                        "javac {src}", "java Main", null, "")
        );
    }

    @Test
    @Order(7)
    @DisplayName("createConfiguration — sanitize-unsafe karakterli isim reddedilir")
    void testCreate_unsafeName_throwsException() {
        // "/" karakteri sanitizeFileName tarafından "_" yapılır → çakışma riski
        assertThrows(IllegalArgumentException.class, () ->
                service.createConfiguration("Test_Bad/Name", "Java", ".java",
                        "javac {src}", "java Main", new ExactMatchStrategy(), ""),
                "İçinde '/' geçen isim red edilmeli (silent overwrite riski)"
        );
    }

    @Test
    @Order(8)
    @DisplayName("createConfiguration — failed save sonrası cache mutate olmamalı (consistency)")
    void testCreate_failedSave_doesNotPolluteCache() {
        // sanitize-unsafe isim → IO katmanında IllegalArgumentException atar
        // Cache mutate olmadığını doğrula
        assertThrows(Exception.class, () ->
                service.createConfiguration("Bad/Name", "Java", ".java",
                        "javac {src}", "java Main", new ExactMatchStrategy(), "")
        );

        assertFalse(service.exists("Bad/Name"),
                "Disk yazımı başarısız olunca cache'de hayalet config kalmamalı");
    }

    // -----------------------------------------------------------------------
    // READ
    // -----------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("getConfiguration — var olan config'i döner")
    void testGet_existingConfig() throws Exception {
        service.createConfiguration("Test_Get", "Python", ".py", "", "python3 {src}",
                new ExactMatchStrategy(), "desc");

        Configuration found = service.getConfiguration("Test_Get");
        assertNotNull(found);
        assertEquals("Python", found.getLanguage());
    }

    @Test
    @Order(10)
    @DisplayName("getConfiguration — olmayan isim için null döner")
    void testGet_nonExisting_returnsNull() {
        assertNull(service.getConfiguration("YokBoyleBirConfig"));
    }

    @Test
    @Order(11)
    @DisplayName("getAllConfigurations — eklenen tüm configler listelenir")
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
    @DisplayName("exists — doğru boolean döner")
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
    @DisplayName("updateConfiguration — alanlar güncellenir")
    void testUpdate_fieldsChanged() throws Exception {
        service.createConfiguration("Test_Upd", "Java", ".java", "javac {src}", "java Main",
                new ExactMatchStrategy(), "eski");

        service.updateConfiguration(
                "Test_Upd", "Test_Upd",
                "Java 17", ".java",
                "javac -source 17 {src}", "java Main",
                new TrimLinesStrategy(), "yeni açıklama"
        );

        Configuration updated = service.getConfiguration("Test_Upd");
        assertEquals("Java 17", updated.getLanguage());
        assertEquals("yeni açıklama", updated.getDescription());
        assertInstanceOf(TrimLinesStrategy.class, updated.getComparisonStrategy());
    }

    @Test
    @Order(14)
    @DisplayName("updateConfiguration — isim değiştirilir, cache güncellenir")
    void testUpdate_renameConfig_updatesCache() throws Exception {
        service.createConfiguration("Test_OldName", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");

        service.updateConfiguration("Test_OldName", "Test_NewName", "C", ".c",
                "gcc {src}", "./{out}", new ExactMatchStrategy(), "");

        assertFalse(service.exists("Test_OldName"), "Eski isim artık olmamalı");
        assertTrue(service.exists("Test_NewName"), "Yeni isim cache'de olmalı");
    }

    @Test
    @Order(15)
    @DisplayName("updateConfiguration — isim değişince eski JSON dosyası diskten silinir (orphan yok)")
    void testUpdate_renameConfig_oldFileDeletedFromDisk() throws Exception {
        service.createConfiguration("Test_Orphan", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");

        File oldFile = new File("config", "Test_Orphan.json");
        File newFile = new File("config", "Test_Renamed.json");
        assertTrue(oldFile.exists(), "Önkoşul: eski dosya disk üstünde olmalı");

        service.updateConfiguration("Test_Orphan", "Test_Renamed", "C", ".c",
                "gcc {src}", "./{out}", new ExactMatchStrategy(), "");

        assertFalse(oldFile.exists(), "Eski dosya orphan kalmamalı, diskten silinmiş olmalı");
        assertTrue(newFile.exists(), "Yeni isimli dosya disk üstünde olmalı");
    }

    @Test
    @Order(16)
    @DisplayName("updateConfiguration — olmayan config güncellenince exception fırlatır")
    void testUpdate_nonExisting_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.updateConfiguration("Test_Hayalet", "Test_Hayalet", "C", ".c",
                        "gcc {src}", "./{out}", new ExactMatchStrategy(), "")
        );
    }

    @Test
    @Order(17)
    @DisplayName("updateConfiguration — null ComparisonStrategy ile exception fırlatır")
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
    @DisplayName("deleteConfiguration — config cache'den kaldırılır")
    void testDelete_removesFromCache() throws Exception {
        service.createConfiguration("Test_Del", "Python", ".py", "", "python3 {src}",
                new ExactMatchStrategy(), "");
        assertTrue(service.exists("Test_Del"));

        service.deleteConfiguration("Test_Del");

        assertFalse(service.exists("Test_Del"), "Silinen config artık cache'de olmamalı");
    }

    @Test
    @Order(19)
    @DisplayName("deleteConfiguration — JSON dosyası diskten silinir (kalıcı silme)")
    void testDelete_removesFileFromDisk() throws Exception {
        service.createConfiguration("Test_HardDel", "Python", ".py", "", "python3 {src}",
                new ExactMatchStrategy(), "");

        File jsonFile = new File("config", "Test_HardDel.json");
        assertTrue(jsonFile.exists(), "Önkoşul: dosya disk üstünde olmalı");

        service.deleteConfiguration("Test_HardDel");

        assertFalse(jsonFile.exists(), "Dosya diskten de silinmiş olmalı");
    }

    @Test
    @Order(20)
    @DisplayName("deleteConfiguration — silinen config yeniden yüklemeden sonra geri gelmemeli")
    void testDelete_doesNotReappearAfterReload() throws Exception {
        service.createConfiguration("Test_NoReappear", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");

        service.deleteConfiguration("Test_NoReappear");
        service.reloadFromDisk();

        assertFalse(service.exists("Test_NoReappear"),
                "Silinen config disk reload sonrası geri gelmemeli");
    }

    @Test
    @Order(21)
    @DisplayName("deleteConfiguration — olmayan config silinince exception fırlatır")
    void testDelete_nonExisting_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.deleteConfiguration("Test_YokBurada")
        );
    }

    // -----------------------------------------------------------------------
    // RELOAD
    // -----------------------------------------------------------------------

    @Test
    @Order(22)
    @DisplayName("reloadFromDisk — kaydedilmemiş cache girdileri silinmeli")
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
        assertTrue(service.exists("Test_GhostInCache"), "Önkoşul: cache'de olmalı");

        service.reloadFromDisk();

        assertFalse(service.exists("Test_GhostInCache"),
                "Reload sonrası diskte olmayan cache girdisi silinmiş olmalı");
    }

    // -----------------------------------------------------------------------
    // ComparisonStrategy korunumu
    // -----------------------------------------------------------------------

    @Test
    @Order(23)
    @DisplayName("IgnoreWhitespaceStrategy config oluşturulunca korunur")
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
