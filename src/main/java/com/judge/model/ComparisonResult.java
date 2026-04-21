package com.judge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ComparisonResult {
    private boolean passed;
    private ComparisonMode comparisonMode;
    private int expectedLength;
    private int actualLength;
}
