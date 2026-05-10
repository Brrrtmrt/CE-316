CREATE TABLE IF NOT EXISTS projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    language TEXT NOT NULL,
    description TEXT,

    -- configuration fields embedded
    config_name TEXT,
    source_dir TEXT,
    output_dir TEXT,
    test_dir TEXT,
    comparison_strategy TEXT,
    extra_param TEXT
);

CREATE TABLE IF NOT EXISTS configurations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    name TEXT,
    source_dir TEXT,
    output_dir TEXT,
    test_dir TEXT,
    comparison_strategy TEXT,
    extra_param TEXT,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS evaluation_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    student_id TEXT NOT NULL,
    unzip_success INTEGER,
    compile_success INTEGER,
    run_success INTEGER,
    output_match INTEGER,
    error_log TEXT,
    status TEXT,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);