package com.iae.domain;

import java.io.File;

public class StudentSubmission {
    private String studentId;
    private File zipFile;

    public StudentSubmission(String studentId, File zipFile) {
        this.studentId = studentId;
        this.zipFile = zipFile;
    }

    public File getZipFile() {
        return null;
    }

    public File getExtractedDir() {
        return null;
    }

    public String getStudentId() {
        return null;
    }

    public File getExecutableFile() {
        return null;
    }

    public void setProgramOutput(String output) {
    }

    public File getSourceFile() {
        return null;
    }

    public String getProgramOutput() {
        return null;
    }
}