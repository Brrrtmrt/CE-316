package com.iae.domain;

import java.io.File;

public class StudentSubmission {
    private final String studentId;
    private final File zipFile;
    private final File extractedDir;
    private File sourceFile;
    private File executableFile;
    private String programOutput;
    private Status submissionStatus;
    private EvaluationResult evaluationResult;

    public StudentSubmission(String studentId, File zipFile) {
        this.studentId = studentId;
        this.zipFile = zipFile;
        String dirName = zipFile.getName().toLowerCase().endsWith(".zip")
                ? zipFile.getName().substring(0, zipFile.getName().length() - 4)
                : zipFile.getName();
        this.extractedDir = new File(zipFile.getParentFile(), dirName);
        this.evaluationResult = new EvaluationResult(studentId);
    }

    public String getStudentId() {
        return studentId;
    }

    public File getZipFile() {
        return zipFile;
    }

    public File getExtractedDir() {
        return extractedDir;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public File getExecutableFile() {
        return executableFile;
    }

    public void setExecutableFile(File executableFile) {
        this.executableFile = executableFile;
    }

    public String getProgramOutput() {
        return programOutput;
    }

    public void setProgramOutput(String programOutput) {
        this.programOutput = programOutput;
    }

    public Status getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(Status submissionStatus) {
        this.submissionStatus = submissionStatus;
    }

    public EvaluationResult getEvaluationResult() {
        return evaluationResult;
    }

    public void setEvaluationResult(EvaluationResult evaluationResult) {
        this.evaluationResult = evaluationResult;
    }
}
