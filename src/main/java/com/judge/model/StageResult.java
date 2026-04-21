package com.judge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class StageResult {
    private boolean executed;
    private Integer exitCode;
    private long durationMillis;
    private boolean timedOut;
    private String stdoutFilePath;
    private String stderrFilePath;
}
