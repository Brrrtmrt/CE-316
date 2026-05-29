package com.iae.util;

import com.iae.domain.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stateless validation helpers shared across the IAE code base.
 *
 * <p>Before this class existed the same checks were copied into
 * {@code ConfigurationService} (blank-field checks) and {@code ConfigurationIO}
 * (illegal file-name characters). Centralising them here keeps the rules
 * consistent and gives callers two complementary styles:
 *
 * <ul>
 *   <li><b>{@code require...}</b> methods throw {@link IllegalArgumentException}
 *       on the first violation - convenient for guard clauses; and</li>
 *   <li><b>{@code isValid...}</b> / {@link #collectConfigurationErrors(Configuration)}
 *       methods return a boolean or a list of messages - convenient for GUI
 *       forms that want to display every problem at once.</li>
 * </ul>
 */
public final class Validator {

    /**
     * Characters that are unsafe in a file name on at least one supported OS:
     * path separators, wildcards, redirection symbols, quotes, colon and
     * control characters. This is intentionally identical to the rule applied
     * by {@code ConfigurationIO}, so a name accepted here is always writable to
     * disk without silent substitution.
     */
    private static final Pattern ILLEGAL_NAME_CHARS =
            Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]");

    /** The comparison methods understood by the persistence layer. */
    private static final Set<String> VALID_COMPARISON_METHODS = Set.of(
            Constants.COMPARISON_EXACT,
            Constants.COMPARISON_IGNORE_WHITESPACE,
            Constants.COMPARISON_TRIM_LINES);

    private Validator() {
        // Utility class - must not be instantiated.
        throw new AssertionError("Validator is a utility class and cannot be instantiated.");
    }

    // -------------------------------------------------------------------------
    // Generic guard clauses
    // -------------------------------------------------------------------------

    /** @return {@code true} if the value is {@code null} or contains only whitespace. */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code value} is blank.
     *
     * @param value     the value to check
     * @param fieldName the human-readable field name used in the error message
     * @return the original {@code value}, for fluent use
     */
    public static String requireNotBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value;
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code value} is {@code null}.
     *
     * @param value     the value to check
     * @param fieldName the human-readable field name used in the error message
     * @param <T>       the value type
     * @return the original {@code value}, for fluent use
     */
    public static <T> T requireNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null.");
        }
        return value;
    }

    // -------------------------------------------------------------------------
    // Configuration name
    // -------------------------------------------------------------------------

    /**
     * @return {@code true} if {@code name} is non-blank and contains no
     *         characters that are illegal in a file name.
     */
    public static boolean isValidConfigurationName(String name) {
        return !isBlank(name) && !ILLEGAL_NAME_CHARS.matcher(name).find();
    }

    /**
     * Validates a configuration name and returns it trimmed.
     *
     * @param name the candidate name
     * @return the trimmed name
     * @throws IllegalArgumentException if the name is blank or contains characters
     *                                  that cannot be written to disk safely
     */
    public static String requireValidConfigurationName(String name) {
        requireNotBlank(name, "Configuration name");
        if (ILLEGAL_NAME_CHARS.matcher(name).find()) {
            throw new IllegalArgumentException(
                    "Configuration name contains characters that are not allowed: '"
                            + name + "'. Disallowed: \\ / : * ? \" < > | and control characters.");
        }
        return name.trim();
    }

    // -------------------------------------------------------------------------
    // File extension
    // -------------------------------------------------------------------------

    /**
     * A file extension is valid if it is empty (interpreted languages need none)
     * or starts with a dot followed by at least one non-whitespace character.
     *
     * @param extension the extension to check (may be {@code null})
     * @return {@code true} if the extension is acceptable
     */
    public static boolean isValidFileExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return true;
        }
        return extension.matches("\\.\\S+");
    }

    /**
     * Normalises a file extension by trimming it and ensuring it begins with a
     * leading dot. A {@code null} or empty input yields an empty string.
     *
     * @param extension the raw extension (e.g. {@code "java"} or {@code ".java"})
     * @return the normalised extension (e.g. {@code ".java"}) or {@code ""}
     */
    public static String normalizeFileExtension(String extension) {
        if (extension == null) {
            return "";
        }
        String trimmed = extension.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.startsWith(".") ? trimmed : "." + trimmed;
    }

    // -------------------------------------------------------------------------
    // Comparison method
    // -------------------------------------------------------------------------

    /**
     * @return {@code true} if {@code method} is one of the comparison-method keys
     *         understood by the persistence layer (case-insensitive).
     */
    public static boolean isValidComparisonMethod(String method) {
        return method != null && VALID_COMPARISON_METHODS.contains(method.toLowerCase());
    }

    // -------------------------------------------------------------------------
    // Whole-Configuration validation
    // -------------------------------------------------------------------------

    /**
     * Inspects every field of a {@link Configuration} and returns a list of
     * human-readable problems. An empty list means the configuration is valid.
     *
     * <p>This non-throwing form is intended for GUI forms that want to show all
     * issues to the user at once.
     *
     * @param config the configuration to inspect (may be {@code null})
     * @return a (possibly empty) list of error messages, never {@code null}
     */
    public static List<String> collectConfigurationErrors(Configuration config) {
        List<String> errors = new ArrayList<>();
        if (config == null) {
            errors.add("Configuration is null.");
            return errors;
        }
        if (!isValidConfigurationName(config.getName())) {
            errors.add("Name is blank or contains illegal characters (\\ / : * ? \" < > |).");
        }
        if (isBlank(config.getLanguage())) {
            errors.add("Language must not be blank.");
        }
        if (isBlank(config.getRunCommand())) {
            errors.add("Run command must not be blank.");
        }
        if (!isValidFileExtension(config.getFileExtension())) {
            errors.add("File extension must be empty or start with a dot (e.g. \".java\").");
        }
        if (config.getComparisonStrategy() == null) {
            errors.add("Comparison strategy must not be null.");
        }
        return errors;
    }

    /** @return {@code true} if the configuration has no validation errors. */
    public static boolean isValidConfiguration(Configuration config) {
        return collectConfigurationErrors(config).isEmpty();
    }

    /**
     * Throws {@link IllegalArgumentException} listing every problem if the
     * configuration is invalid; returns normally otherwise.
     *
     * @param config the configuration to validate
     * @throws IllegalArgumentException if one or more fields are invalid
     */
    public static void requireValidConfiguration(Configuration config) {
        List<String> errors = collectConfigurationErrors(config);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid configuration: " + String.join(" ", errors));
        }
    }
}
