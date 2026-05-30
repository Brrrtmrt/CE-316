package com.iae.evaluation.steps;

import com.iae.domain.Configuration;
import com.iae.domain.StudentSubmission;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class UnzipStepTest {

    @TempDir
    Path tempDir;

    private Configuration config(String extension) {
        return new Configuration("Test", "Java", extension, "javac {src}", "java Main", new ExactMatchStrategy(), "test");
    }

    private File createZip(String zipName, String entryName, String content) throws IOException {
        File zip = tempDir.resolve(zipName).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(content.getBytes());
            zos.closeEntry();
        }
        return zip;
    }

    @Test
    void nullSubmission_returnsError() {
        StepResult result = new UnzipStep(config(".java")).execute(null);
        assertFalse(result.isSuccess());
        assertEquals("UNZIP", result.getStepName());
    }

    @Test
    void zipFileDoesNotExist_returnsError() {
        StudentSubmission sub = new StudentSubmission("s1", tempDir.resolve("missing.zip").toFile());
        assertFalse(new UnzipStep(config(".java")).execute(sub).isSuccess());
    }

    @Test
    void fileNotEndingInZip_returnsError() throws IOException {
        File notZip = tempDir.resolve("student.txt").toFile();
        notZip.createNewFile();
        assertFalse(new UnzipStep(config(".java")).execute(new StudentSubmission("s1", notZip)).isSuccess());
    }

    @Test
    void successfulExtraction_returnsSuccess() throws IOException {
        File zip = createZip("student.zip", "Main.java", "public class Main {}");
        StepResult result = new UnzipStep(config(".java")).execute(new StudentSubmission("s1", zip));
        assertTrue(result.isSuccess());
        assertEquals("UNZIP", result.getStepName());
    }

    @Test
    void successfulExtraction_setsSourceAndExecutableFile() throws IOException {
        File zip = createZip("student.zip", "Main.java", "public class Main {}");
        StudentSubmission sub = new StudentSubmission("s1", zip);
        new UnzipStep(config(".java")).execute(sub);
        assertNotNull(sub.getSourceFile());
        assertTrue(sub.getSourceFile().getName().endsWith(".java"));
        assertNotNull(sub.getExecutableFile());
    }

    @Test
    void noMatchingSourceFile_returnsFailure() throws IOException {
        File zip = createZip("student.zip", "readme.txt", "some text");
        assertFalse(new UnzipStep(config(".java")).execute(new StudentSubmission("s1", zip)).isSuccess());
    }

    @Test
    void extensionWithoutLeadingDot_stillMatches() throws IOException {
        File zip = createZip("student.zip", "Main.java", "public class Main {}");
        assertTrue(new UnzipStep(config("java")).execute(new StudentSubmission("s1", zip)).isSuccess());
    }
}
