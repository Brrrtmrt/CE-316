package com.iae.evaluation.steps;

import com.iae.domain.StudentSubmission;
import com.iae.infrastructure.ZipHandler;

public class UnzipStep extends AbstractEvaluationStep {

    private final ZipHandler zipHandler;

    public UnzipStep(ZipHandler zipHandler) {
        this.zipHandler = zipHandler;
    }

    @Override
    protected void validate(StudentSubmission submission) throws Exception {
        super.validate(submission);

        if (!submission.getZipFile().exists()) {
            throw new IllegalArgumentException("ZIP file does not exist: " + submission.getZipFile());
        }

        if (!submission.getZipFile().getName().endsWith(".zip")) {
            throw new IllegalArgumentException("File is not a ZIP file: " + submission.getZipFile());
        }
    }

    @Override
    protected StepResult doExecute(StudentSubmission submission) throws Exception {
        zipHandler.extract(submission.getZipFile(), submission.getExtractedDir());

        return StepResult.success(getStepName(),
                "Successfully extracted ZIP file for student: " + submission.getStudentId());
    }

    @Override
    protected String getStepName() {
        return "UNZIP";
    }
}
