package com.iae.evaluation.steps;

import com.iae.domain.StudentSubmission;
import com.iae.evaluation.strategies.ComparisonStrategy;

public class CompareStep extends AbstractEvaluationStep {

    private final ComparisonStrategy comparisonStrategy;
    private final String expectedOutput;

    public CompareStep(ComparisonStrategy comparisonStrategy, String expectedOutput) {
        this.comparisonStrategy = comparisonStrategy;
        this.expectedOutput = expectedOutput;
    }

    @Override
    protected void validate(StudentSubmission submission) throws Exception {
        super.validate(submission);

        if (submission.getProgramOutput() == null) {
            throw new IllegalStateException("Program output is null. Run step may have failed.");
        }
    }

    @Override
    protected StepResult doExecute(StudentSubmission submission) throws Exception {
        String actualOutput = submission.getProgramOutput();

        boolean matches = comparisonStrategy.compare(actualOutput, expectedOutput);

        if (matches) {
            return StepResult.success(getStepName(),
                    "Output matches expected result for student: " + submission.getStudentId());
        } else {
            return StepResult.failure(getStepName(),
                    "Output does not match expected result");
        }
    }

    @Override
    protected String getStepName() {
        return "COMPARE";
    }
}
