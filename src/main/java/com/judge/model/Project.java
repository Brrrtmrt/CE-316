package com.judge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Project {
    private String name;
    private String description;
    private String projectDir; // Where to save the project.
    private UUID configurationId; // Reference to the configuration.
    private String submissionsDir; // Where the submissions(ZIP Files) live.
    private String entryPoint; // Specify main class name.
    private InputMode inputMode;
    private String inputData;
    private String expectedOutput;
    private ComparisonMode comparisonMode;
    private List<Submission> submissions;
    private LocalDateTime lastRunAt;
    private LocalDateTime createdAt;
    private int timeoutSeconds;

    public void addSubmission(Submission submission) {
        submissions.add(submission);
    }

}
