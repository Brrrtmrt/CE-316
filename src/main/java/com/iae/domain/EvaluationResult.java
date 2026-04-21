package com.iae.domain;

public class EvaluationResult {
    private String studentId;

    public EvaluationResult(String studentId) {
        this.studentId = studentId;
    }

    public static EvaluationResult error(String studentId, String s) {
        return null;
    }

    public void setUnzipSuccess(boolean success) {
    }

    public void setErrorLog(String errorDetails) {
    }

    public void setCompileSuccess(boolean success) {

    }

    public void setRunSuccess(boolean success) {
    }

    public void setOutputMatch(boolean success) {
    }
}