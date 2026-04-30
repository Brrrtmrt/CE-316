package com.iae.infrastructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZipHandler
 *
 * <p>This class provides secure ZIP file extraction. All methods are static as this is
 * a stateless utility class.</p>
 *
 * <h2>Security Features:</h2>
 * <ul>
 *   <li>Zip Slip attack prevention - validates all extracted paths</li>
 *   <li>Canonical path verification - prevents directory traversal</li>
 *   <li>Safe file overwriting - replaces existing files</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * File zipFile = new File("student123.zip");
 * File targetDir = new File("/tmp/student123");
 *
 * if (ZipHandler.extract(zipFile, targetDir)) {
 *     System.out.println("Extraction successful");
 * } else {
 *     System.err.println("Extraction failed");
 * }
 * }</pre>
 *
 * <h2>MT-Safe:</h2>
 * <p>This class is thread-safe as all methods are static and stateless.
 * Multiple threads can safely call extract() simultaneously on <strong>different</strong> files.</p>
 *
 * @author MS
 * @version 1.0
 * @since 0.0
 */
public class ZipHandler {

    private ZipHandler() {
        throw new UnsupportedOperationException("Do not instantiate me.");
    }

    /**
     * Extracts a ZIP file to the specified directory.
     *
     * @param zipFile      the ZIP file to extract (must exist and be a regular file)
     * @param extractedDir the target directory for extraction (will be created if needed)
     * @return true if extraction completed successfully, false if any error occurred
     * @throws NullPointerException if zipFile or extractedDir is null
     * @see #isWithinDirectory(File, File)
     */
    public static boolean extract(File zipFile, File extractedDir) {

        if (zipFile == null || extractedDir == null) {
            System.err.println("ZIP file and extraction directory cannot be null");
            return false;
        }

        if (!zipFile.exists() || !zipFile.isFile()) {
            System.err.println("ZIP file does not exist or is not a file: " + zipFile.getAbsolutePath());
            return false;
        }

        if (!zipFile.canRead()) {
            System.err.println("ZIP file is not readable: " + zipFile.getAbsolutePath());
            return false;
        }

        if (!extractedDir.exists() && !extractedDir.mkdirs()) {
            System.err.println("Failed to create extraction directory: " + extractedDir.getAbsolutePath());
            return false;
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(extractedDir, entry.getName());

                // Prevent Zip Slip attack
                if (!isWithinDirectory(extractedDir, file)) {
                    System.err.println("Error: " + entry.getName());
                    return false;
                }

                if (entry.isDirectory()) {
                    if (!file.exists() && !file.mkdirs()) {
                        return false;
                    }
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        return false;
                    }
                    // Extract file with overwrite
                    Files.copy(zis, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }

            return true;

        } catch (IOException e) {
            System.err.println("Failed to extract ZIP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Security check to prevent Zip Slip vulnerability.
     *
     * <p>Zip Slip is a vulnerability that allows attackers to write files outside
     * the target directory by using "../" sequences in ZIP entry names. This method
     * validates that the extracted file's canonical path is within the target directory.</p>
     *
     * @param targetDir the intended extraction directory
     * @param file      the file to be extracted
     * @return true if file is within target directory, false otherwise
     */
    private static boolean isWithinDirectory(File targetDir, File file) {
        try {
            String targetPath = targetDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            return filePath.startsWith(targetPath + File.separator) || filePath.equals(targetPath);
        } catch (IOException e) {
            return false;
        }
    }
}
