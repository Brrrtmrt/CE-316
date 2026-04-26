package com.iae.domain;

public enum Status {
    PASS,    // All steps succeeded, output matches expected.
    FAIL,    // Compilation/execution succeeded but output doesn't match.
    ERROR    // Compilation error, runtime error, or ZIP extraction failed.
}
