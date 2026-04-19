package com.iae.evaluation.steps;

import com.iae.domain.StudentSubmission;

public abstract class AbstractEvaluationStep {

    public final StepResult execute(StudentSubmission submission) {
        try {
            validate(submission);
            StepResult result = doExecute(submission);
            cleanup(submission);

            return result;

        } catch (Exception e) {
            return StepResult.error(getStepName(), e.getMessage());
        }
    }

    protected void validate(StudentSubmission submission) throws Exception {
        if (submission == null) {
            throw new IllegalArgumentException("Submission cannot be null");
        }
    }

    protected abstract StepResult doExecute(StudentSubmission submission) throws Exception;

    protected void cleanup(StudentSubmission submission) {
    }

    protected abstract String getStepName();
}
