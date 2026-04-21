package com.iae.evaluation.steps;

public class StepResult {
    private final boolean success;
    private final String stepName;
    private final String message;
    private final String errorDetails;
    
    private StepResult(boolean success, String stepName, String message, String errorDetails) {
        this.success = success;
        this.stepName = stepName;
        this.message = message;
        this.errorDetails = errorDetails;
    }
    
    public static StepResult success(String stepName, String message) {
        return new StepResult(true, stepName, message, null);
    }
    
    public static StepResult failure(String stepName, String message) {
        return new StepResult(false, stepName, message, null);
    }
    
    public static StepResult error(String stepName, String errorDetails) {
        return new StepResult(false, stepName, "Error occurred", errorDetails);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getStepName() {
        return stepName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", stepName, success ? "SUCCESS" : "FAILURE", message);
    }
}
