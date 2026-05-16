package com.iae.evaluation.steps;

import com.iae.domain.Configuration;
import com.iae.domain.StudentSubmission;
import com.iae.infrastructure.ZipHandler;

import java.io.File;
import java.util.logging.Logger;

public class UnzipStep extends AbstractEvaluationStep {

    private static final Logger logger = Logger.getLogger(UnzipStep.class.getName());

    private final Configuration configuration;

    public UnzipStep(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void validate(StudentSubmission submission) throws Exception {
        super.validate(submission);

        if (!submission.getZipFile().exists()) {
            throw new IllegalArgumentException("ZIP file does not exist: " + submission.getZipFile());
        }

        if (!submission.getZipFile().getName().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("File is not a ZIP file: " + submission.getZipFile());
        }
    }

    @Override
    protected StepResult doExecute(StudentSubmission submission) throws Exception {
        logger.info("Extracting ZIP for student: " + submission.getStudentId());

        boolean extracted = ZipHandler.extract(submission.getZipFile(), submission.getExtractedDir());
        if (!extracted) {
            return StepResult.failure(getStepName(),
                    "ZIP extraction failed for student: " + submission.getStudentId());
        }

        logger.info("Searching for source file in: " + submission.getExtractedDir());

        String extension = configuration.getFileExtension();
        File sourceFile = findSourceFile(submission.getExtractedDir(), extension);
        if (sourceFile == null) {
            return StepResult.failure(getStepName(),
                    "No source file with extension '" + extension + "' found for student: "
                            + submission.getStudentId());
        }

        submission.setSourceFile(sourceFile);
        logger.info("Found source file: " + sourceFile.getAbsolutePath());

        String execName = System.getProperty("os.name").toLowerCase().contains("win") ? "main.exe" : "main";
        submission.setExecutableFile(new File(submission.getExtractedDir(), execName));

        return StepResult.success(getStepName(),
                "Extracted and located source for student: " + submission.getStudentId());
    }

    @Override
    protected String getStepName() {
        return "UNZIP";
    }

    /**
     * Recursively searches a directory for the first file matching the given extension.
     * Handles extensions with or without a leading dot (e.g. ".c" or "c").
     */
    private File findSourceFile(File dir, String extension) {
        if (dir == null || !dir.isDirectory()) {
            return null;
        }

        String suffix = extension.startsWith(".") ? extension : "." + extension;

        File[] entries = dir.listFiles();
        if (entries == null) {
            return null;
        }

        for (File entry : entries) {
            if (entry.isDirectory()) {
                File found = findSourceFile(entry, extension);
                if (found != null) {
                    return found;
                }
            } else if (entry.getName().endsWith(suffix)) {
                return entry;
            }
        }

        return null;
    }
}
