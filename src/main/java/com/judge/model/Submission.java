package com.judge.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class Submission {
    private String id;
    private String filePath;
    private String fileName;
    private String extractionPath;

    private SubmissionStatus submissionStatus;
    private Stage stage;

    private List<String> sourceFilesFound;

    private StageResult compileResult;
    private StageResult runResult;
    private ComparisonResult comparisonResult;

    private LocalDateTime processedAt;

}
