CREATE TABLE IF NOT EXISTS projects (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    name                   TEXT    NOT NULL,
    config_name            TEXT    NOT NULL,
    language               TEXT    NOT NULL,
    file_extension         TEXT    NOT NULL,
    compile_command        TEXT,
    run_command            TEXT    NOT NULL,
    comparison_strategy    TEXT    NOT NULL DEFAULT 'exact',
    description            TEXT,
    submissions_directory  TEXT    NOT NULL,
    program_arguments      TEXT,
    expected_output        TEXT
);

CREATE TABLE IF NOT EXISTS evaluation_results (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id       INTEGER NOT NULL,
    student_id       TEXT    NOT NULL,
    unzip_success    INTEGER NOT NULL DEFAULT 0,
    compile_success  INTEGER NOT NULL DEFAULT 0,
    run_success      INTEGER NOT NULL DEFAULT 0,
    output_match     INTEGER NOT NULL DEFAULT 0,
    error_log        TEXT,
    program_output   TEXT,
    status           TEXT    NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
