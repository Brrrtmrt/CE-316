package com.iae.persistence.dao;   // FIXED: was "com.iae.persistence." (trailing dot)

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.domain.Project;
import com.iae.evaluation.strategies.ComparisonStrategy;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.evaluation.strategies.IgnoreWhitespaceStrategy;
import com.iae.evaluation.strategies.TrimLinesStrategy;
import com.iae.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ProjectDAO — full CRUD for the {@code projects} table.
 *
 * <h2>Fixes applied</h2>
 * <ul>
 *   <li>Corrected package declaration (trailing dot removed).</li>
 *   <li>{@code mapConfiguration()} now uses {@link ConfigurationBuilder} with
 *       the correct field names that match both the domain class and the
 *       schema columns ({@code config_name}, {@code language},
 *       {@code file_extension}, {@code compile_command}, {@code run_command},
 *       {@code comparison_strategy}, {@code description}).</li>
 *   <li>All SQL column names now match {@code schema.sql} exactly:
 *       {@code submissions_directory}, {@code program_arguments},
 *       {@code expected_output}.</li>
 *   <li>Added {@code save}, {@code update}, {@code delete}, {@code findAll}
 *       to complete the required CRUD surface.</li>
 * </ul>
 *
 * @author Dev 1
 * @version 1.1
 */
public class ProjectDAO extends BaseDAO {



    /**
     * Finds a project by its primary key.
     *
     * @param id the project's database id
     * @return the {@link Project}, or {@code null} if not found
     * @throws SQLException on any database error
     */
    public Project findById(int id) throws SQLException {
        String sql = "SELECT * FROM projects WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapProject(rs);
                }
            }
        }
        return null;
    }

    /**
     * Returns all projects stored in the database.
     *
     * @return list of projects (empty if none)
     * @throws SQLException on any database error
     */
    public List<Project> findAll() throws SQLException {
        String sql = "SELECT * FROM projects";
        List<Project> projects = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                projects.add(mapProject(rs));
            }
        }
        return projects;
    }



    /**
     * Inserts a new project row and sets the generated id back on the domain
     * object via {@link Project#setId(String)}.
     *
     * @param project the project to persist
     * @throws SQLException on any database error
     */
    public void save(Project project) throws SQLException {
        String sql = """
                INSERT INTO projects
                    (name, config_name, language, file_extension,
                     compile_command, run_command, comparison_strategy, description,
                     submissions_directory, program_arguments, expected_output)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            Configuration cfg = project.getConfiguration();
            stmt.setString(1,  project.getName());
            stmt.setString(2,  cfg.getName());
            stmt.setString(3,  cfg.getLanguage());
            stmt.setString(4,  cfg.getFileExtension());
            stmt.setString(5,  cfg.getCompileCommand());          // may be null (Python)
            stmt.setString(6,  cfg.getRunCommand());
            stmt.setString(7,  strategyToKey(cfg.getComparisonStrategy()));
            stmt.setString(8,  cfg.getDescription());
            stmt.setString(9,  project.getSubmissionsDirectory());
            stmt.setString(10, joinArgs(project.getProgramArguments()));
            stmt.setString(11, project.getExpectedOutput());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    project.setId(String.valueOf(keys.getInt(1)));
                }
            }
        }
    }

    /**
     * Updates an existing project row.  The project must already have a
     * non-null id (i.e. was previously returned by {@link #findById} or
     * persisted via {@link #save}).
     *
     * @param project the project with updated field values
     * @throws SQLException on any database error
     * @throws IllegalArgumentException if the project has no id
     */
    public void update(Project project) throws SQLException {
        if (project.getId() == null) {
            throw new IllegalArgumentException("Cannot update a project with no id");
        }

        String sql = """
                UPDATE projects SET
                    name                  = ?,
                    config_name           = ?,
                    language              = ?,
                    file_extension        = ?,
                    compile_command       = ?,
                    run_command           = ?,
                    comparison_strategy   = ?,
                    description           = ?,
                    submissions_directory = ?,
                    program_arguments     = ?,
                    expected_output       = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Configuration cfg = project.getConfiguration();
            stmt.setString(1,  project.getName());
            stmt.setString(2,  cfg.getName());
            stmt.setString(3,  cfg.getLanguage());
            stmt.setString(4,  cfg.getFileExtension());
            stmt.setString(5,  cfg.getCompileCommand());
            stmt.setString(6,  cfg.getRunCommand());
            stmt.setString(7,  strategyToKey(cfg.getComparisonStrategy()));
            stmt.setString(8,  cfg.getDescription());
            stmt.setString(9,  project.getSubmissionsDirectory());
            stmt.setString(10, joinArgs(project.getProgramArguments()));
            stmt.setString(11, project.getExpectedOutput());
            stmt.setInt(12,    Integer.parseInt(project.getId()));

            stmt.executeUpdate();
        }
    }

    /**
     * Deletes a project and — via the {@code ON DELETE CASCADE} FK constraint —
     * all its associated {@code evaluation_results} rows.
     *
     * @param id the project's database id
     * @throws SQLException on any database error
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM projects WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }



    /**
     * Maps a {@link ResultSet} row to a {@link Project} domain object.
     * Column names match {@code schema.sql} exactly.
     */
    private Project mapProject(ResultSet rs) throws SQLException {
        Configuration config = mapConfiguration(rs);

        String argsRaw = rs.getString("program_arguments");
        String[] args  = (argsRaw != null && !argsRaw.isBlank())
                ? argsRaw.split(",")
                : new String[0];

        Project project = new Project(
                config,
                rs.getString("submissions_directory"),
                args,
                rs.getString("expected_output")
        );

        project.setId(String.valueOf(rs.getInt("id")));
        project.setName(rs.getString("name"));
        return project;
    }

    /**
     * Builds a {@link Configuration} from the embedded columns in the
     * {@code projects} table.
     *
     * <p>FIXED: previously passed wrong fields to the {@code Configuration}
     * constructor ({@code source_dir}, {@code output_dir}, {@code test_dir}
     * which do not exist in the domain model or schema).  Now uses
     * {@link ConfigurationBuilder} with the correct field names.</p>
     */
    private Configuration mapConfiguration(ResultSet rs) throws SQLException {
        ComparisonStrategy strategy = resolveStrategy(
                rs.getString("comparison_strategy"));

        return new ConfigurationBuilder()
                .setName(rs.getString("config_name"))
                .setLanguage(rs.getString("language"))
                .setFileExtension(rs.getString("file_extension"))
                .setCompileCommand(rs.getString("compile_command"))   // may be null
                .setRunCommand(rs.getString("run_command"))
                .setComparisonStrategy(strategy)
                .setDescription(rs.getString("description"))
                .build();
    }



    private ComparisonStrategy resolveStrategy(String key) {
        if (key == null) return new ExactMatchStrategy();
        return switch (key.toLowerCase()) {
            case "ignore_whitespace" -> new IgnoreWhitespaceStrategy();
            case "trim_lines"        -> new TrimLinesStrategy();
            default                  -> new ExactMatchStrategy();
        };
    }

    private String strategyToKey(ComparisonStrategy strategy) {
        if (strategy instanceof IgnoreWhitespaceStrategy) return "ignore_whitespace";
        if (strategy instanceof TrimLinesStrategy)        return "trim_lines";
        return "exact";
    }



    /** Joins a {@code String[]} to a comma-separated value for storage. */
    private String joinArgs(String[] args) {
        if (args == null || args.length == 0) return null;
        return String.join(",", args);
    }
}