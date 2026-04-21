package com.judge.model;

public enum SubmissionStatus {
    PASS,
    FAIL,

    RUNTIME_ERROR,
    TIMED_OUT,

    COMPILE_ERROR,

    NO_SOURCE_FILE,
    ENTRY_POINT_NOT_FOUND,

    EXTRACTION_CORRUPTED,

    SKIPPED,

    INTERNAL_ERROR

}
