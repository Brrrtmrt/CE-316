package com.iae.domain;

public class EvaluationResult {

    private final String studentId;
    private boolean unzipSuccess;
    private boolean compileSuccess;
    private boolean runSuccess;
    private boolean outputMatch;
    private String errorLog;
    private String programOutput;

    public EvaluationResult(String studentId) {
        this.studentId = studentId;
    }

    public static EvaluationResult error(String studentId, String message) {
        EvaluationResult result = new EvaluationResult(studentId);
        result.setErrorLog(message);
        return result;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setUnzipSuccess(boolean unzipSuccess) {
        this.unzipSuccess = unzipSuccess;
    }

    public boolean isUnzipSuccess() {
        return unzipSuccess;
    }

    public void setCompileSuccess(boolean compileSuccess) {
        this.compileSuccess = compileSuccess;
    }

    public boolean isCompileSuccess() {
        return compileSuccess;
    }

    public void setRunSuccess(boolean runSuccess) {
        this.runSuccess = runSuccess;
    }

    public boolean isRunSuccess() {
        return runSuccess;
    }

    public void setOutputMatch(boolean outputMatch) {
        this.outputMatch = outputMatch;
    }

    public boolean isOutputMatch() {
        return outputMatch;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setProgramOutput(String programOutput) {
        this.programOutput = programOutput;
    }

    public String getProgramOutput() {
        return programOutput;
    }

    /**
     * Derives the overall status from the step outcomes.
     * ERROR  — any step before comparison failed (unzip / compile / run).
     * PASS   — all steps succeeded and output matches.
     * FAIL   — execution succeeded but output does not match.
     */
    public Status getStatus() {
        if (!unzipSuccess || !compileSuccess || !runSuccess) {
            return Status.ERROR;
        }
        return outputMatch ? Status.PASS : Status.FAIL;
    }

    @Override
    public String toString() {
        return "EvaluationResult{studentId='" + studentId + "', status=" + getStatus()
                + ", error='" + errorLog + "'}";
    }
}
