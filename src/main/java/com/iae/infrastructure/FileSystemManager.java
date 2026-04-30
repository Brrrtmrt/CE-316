package com.iae.infrastructure;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;


/**
 * FileSystemManager
 *
 * <p>This class is responsible for creating and managing temporary directories
 * used during student submission evaluation. Each instance tracks its own set
 * of temporary directories and provides cleanup functionality.</p>
 *
 * <h2>Functionality:</h2>
 * <ul>
 *   <li>Create temporary directories with sanitized student IDs</li>
 *   <li>Track all created temporary directories for cleanup</li>
 *   <li>Recursively delete temporary directories and their contents</li>
 *   <li>Validate file and directory accessibility</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * FileSystemManager fsm = new FileSystemManager();
 * try {
 *     File tmp = fsm.createTempDirectory("student");
 *     // Use tmp for extraction and compilation etc.
 * } finally {
 *     fsm.cleanupAll(); // Clean up all temp directories.
 * }
 * }</pre>
 *
 * <h2>MT-Unsafe:</h2>
 * <p>This class is <strong>NOT</strong> thread-safe. Each evaluation session should have its own
 * instance. Do not share instances across threads.</p>
 *
 * @author MS
 * @version 1.0
 * @since 0.0
 */
public class FileSystemManager {

    private final List<File> tempdirs;

    private static final String regex = "[^a-zA-Z0-9_-]";

    public FileSystemManager() {
        tempdirs = new ArrayList<>();
    }

    /**
     * Creates a temporary directory for a student submission.
     *
     * <p>The directory is created in the system's temporary directory with a name
     * format of: {@code iae_<sanitizedStudentID>_<timestamp>}</p>
     *
     * <p>The student ID is sanitized to remove invalid characters
     * for file system paths. Timestamp is added to prevent collisions.</p>
     *
     * @param studentID the student identifier (will be sanitized)
     * @return the created temporary directory
     * @throws IOException              if the directory cannot be created
     * @throws IllegalArgumentException if studentID is null
     * @see #sanitizeStudentID(String)
     */
    public File createTempDirectory(String studentID) throws IOException {
        String sanitizedID = sanitizeStudentID(studentID);
        File dir = new File(System.getProperty("java.io.tmpdir"), "iae_" + sanitizedID + "_" + System.currentTimeMillis());

        if (!dir.mkdirs() && !dir.exists()) {
            throw new IOException("Failed to create temporary directory: " + dir.getAbsolutePath());
        }
        tempdirs.add(dir);
        return dir;
    }

    /**
     * Cleans up all temporary directories created by this instance.
     *
     * <p>Attempts to delete each tracked directory and all its contents recursively.
     * If deletion fails for any directory, an error message is printed to stderr,
     * but cleanup continues for remaining directories.</p>
     *
     * <p>After cleanup, the internal tracking list is cleared.</p>
     *
     * <p><strong>Note:</strong> This method should be called in a finally block
     * to ensure cleanup happens even if evaluation fails.</p>
     *
     * @see #del(File)
     */
    public void cleanupAll() {
        for (File dir : tempdirs) {
            try {
                del(dir);
            } catch (IOException e) {
                System.err.println("Failed to delete directory: " + dir.getAbsolutePath() + " - " + e.getMessage());
            }
        }
        tempdirs.clear();
    }

    /**
     * Checks if a file exists and is readable.
     *
     * @param file the file to check
     * @return true if file exists, is a regular file, and is readable; false otherwise
     */
    public boolean isFileReadable(File file) {
        return file != null && file.exists() && file.isFile() && file.canRead();
    }

    /**
     * Checks if a directory exists and is writable.
     *
     * @param directory the directory to check
     * @return true if directory exists, is a directory, and is writable; false otherwise
     */
    public boolean isDirectoryWritable(File directory) {
        return directory != null && directory.exists() && directory.isDirectory() && directory.canWrite();
    }

    /**
     * Ensures a directory exists, creating it if necessary.
     *
     * <p>If the directory already exists, this method does nothing.
     * If it doesn't exist, it attempts to create it along with any necessary
     * parent directories.</p>
     *
     * @param directory the directory to ensure exists
     * @throws IOException if the directory doesn't exist and cannot be created
     */
    public void ensureDirectoryExists(File directory) throws IOException {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
            }
        }
    }

    /**
     * Gets the number of temporary directories currently tracked by this instance.
     *
     * @return the count of temporary directories
     */
    public int getTempDirectoryCount() {
        return tempdirs.size();
    }

    /**
     * Recursively deletes a directory and all its contents.
     *
     * <p>Uses Java NIO's {@link Files#walkFileTree} to traverse the directory tree
     * and delete all files and subdirectories. Deletion is performed in post-order
     * (files first, then directories).</p>
     *
     * <p>If the directory doesn't exist, this method returns without error.</p>
     *
     * @param directory the directory to delete
     * @throws IOException if any file or directory cannot be deleted
     */
    private void del(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        Path path = directory.toPath();
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Sanitizes a student ID to be safe for file system use.
     *
     * <p>Removes or replaces characters that are invalid or problematic in file paths.
     * Only alphanumeric characters, underscores, and hyphens are allowed.
     * All other characters are replaced with underscores.</p>
     *
     * <p>If the student ID is null or empty, returns "unknown".</p>
     *
     * @param studentID the student ID to sanitize
     * @return the sanitized student ID safe for use in file paths
     * @example <pre>
     * sanitizeStudentID("student@123")  → "student_123"
     * sanitizeStudentID("john.doe")     → "john_doe"
     * sanitizeStudentID(null)           → "unknown"
     * sanitizeStudentID("")             → "unknown"
     * </pre>
     */
    private static String sanitizeStudentID(String studentID) {
        if (studentID == null || studentID.trim().isEmpty()) {
            return "unknown";
        }
        return studentID.replaceAll(regex, "_");
    }

}
