package com.iae.domain;

/**
 * Holds the outcome of every pipeline step for a single student submission.
 * <p>
 * The four boolean flags mirror the four evaluation pipeline steps:
 * unzip → compile → run → outputMatch. Together with the {@link Status}
 * enum they give callers an at-a-glance summary of what went wrong.
 */
public class EvaluationResult {

    private final String studentId;

    private boolean unzipSuccess;
    private boolean compileSuccess;
    private boolean runSuccess;
    private boolean outputMatch;
    private String  errorLog;

    // -----------------------------------------------------------------------
    // Constructors / static factories
    // -----------------------------------------------------------------------

    public EvaluationResult(String studentId) {
        this.studentId      = studentId;
        this.unzipSuccess   = false;
        this.compileSuccess = false;
        this.runSuccess     = false;
        this.outputMatch    = false;
        this.errorLog       = null;
    }

    /**
     * Convenience factory that creates a pre-failed result with an error message.
     * Used by pipeline steps when an unexpected exception is caught.
     *
     * @param studentId the student whose submission caused the error
     * @param message   human-readable description of what went wrong
     * @return a new {@code EvaluationResult} with the error log already set
     */
    public static EvaluationResult error(String studentId, String message) {
        EvaluationResult result = new EvaluationResult(studentId);
        result.setErrorLog(message);
        return result;
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getStudentId() {
        return studentId;
    }

    public boolean isUnzipSuccess() {
        return unzipSuccess;
    }

    public boolean isCompileSuccess() {
        return compileSuccess;
    }

    public boolean isRunSuccess() {
        return runSuccess;
    }

    public boolean isOutputMatch() {
        return outputMatch;
    }

    public String getErrorLog() {
        return errorLog;
    }

    /**
     * Derives the overall {@link Status} from the individual step flags.
     * <ul>
     *   <li>{@code PASS}  – all four steps succeeded</li>
     *   <li>{@code FAIL}  – compile and run succeeded but output does not match</li>
     *   <li>{@code ERROR} – unzip, compile, or run step failed</li>
     * </ul>
     */
    public Status deriveStatus() {
        if (!unzipSuccess || !compileSuccess || !runSuccess) {
            return Status.ERROR;
        }
        return outputMatch ? Status.PASS : Status.FAIL;
    }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    public void setUnzipSuccess(boolean unzipSuccess) {
        this.unzipSuccess = unzipSuccess;
    }

    public void setCompileSuccess(boolean compileSuccess) {
        this.compileSuccess = compileSuccess;
    }

    public void setRunSuccess(boolean runSuccess) {
        this.runSuccess = runSuccess;
    }

    public void setOutputMatch(boolean outputMatch) {
        this.outputMatch = outputMatch;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    // -----------------------------------------------------------------------
    // Object
    // -----------------------------------------------------------------------

    @Override
    public String toString() {
        return "EvaluationResult{"
                + "studentId='" + studentId + '\''
                + ", unzip="   + unzipSuccess
                + ", compile=" + compileSuccess
                + ", run="     + runSuccess
                + ", match="   + outputMatch
                + ", status="  + deriveStatus()
                + (errorLog != null ? ", error='" + errorLog + '\'' : "")
                + '}';
    }
}