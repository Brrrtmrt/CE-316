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
    @DisplayName("getConfiguration — var olan config'i döner")
    void testGet_existingConfig() throws Exception {
        service.createConfiguration("Test_Get", "Python", ".py", "", "python3 {src}",
                new ExactMatchStrategy(), "desc");

        Configuration found = service.getConfiguration("Test_Get");
        assertNotNull(found);
        assertEquals("Python", found.getLanguage());
    }

    @Test
    @Order(7)
    @DisplayName("getConfiguration — olmayan isim için null döner")
    void testGet_nonExisting_returnsNull() {
        assertNull(service.getConfiguration("YokBoyleBirConfig"));
    }

    @Test
    @Order(8)
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
    @Order(9)
    @DisplayName("exists — doğru boolean döner")
    void testExists() throws Exception {
        assertFalse(service.exists("Test_Var"));
        service.createConfiguration("Test_Var", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");
        assertTrue(service.exists("Test_Var"));
    }



    @Test
    @Order(10)
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
    @Order(11)
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
    @Order(12)
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
    @Order(13)
    @DisplayName("updateConfiguration — olmayan config güncellenince exception fırlatır")
    void testUpdate_nonExisting_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.updateConfiguration("Test_Hayalet", "Test_Hayalet", "C", ".c",
                        "gcc {src}", "./{out}", new ExactMatchStrategy(), "")
        );
    }



    @Test
    @Order(14)
    @DisplayName("deleteConfiguration — config cache'den kaldırılır")
    void testDelete_removesFromCache() throws Exception {
        service.createConfiguration("Test_Del", "Python", ".py", "", "python3 {src}",
                new ExactMatchStrategy(), "");
        assertTrue(service.exists("Test_Del"));

        service.deleteConfiguration("Test_Del");

        assertFalse(service.exists("Test_Del"), "Silinen config artık cache'de olmamalı");
    }

    @Test
    @Order(15)
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
    @Order(16)
    @DisplayName("deleteConfiguration — silinen config yeniden başlatmadan sonra geri gelmemeli")
    void testDelete_doesNotReappearAfterReload() throws Exception {
        service.createConfiguration("Test_NoReappear", "C", ".c", "gcc {src}", "./{out}",
                new ExactMatchStrategy(), "");

        service.deleteConfiguration("Test_NoReappear");

        service.reloadFromDisk();
        assertFalse(service.exists("Test_NoReappear"),
                "Silinen config disk reload sonrası geri gelmemeli");
    }

    @Test
    @Order(17)
    @DisplayName("deleteConfiguration — olmayan config silinince exception fırlatır")
    void testDelete_nonExisting_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.deleteConfiguration("Test_YokBurada")
        );
    }


    @Test
    @Order(18)
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
