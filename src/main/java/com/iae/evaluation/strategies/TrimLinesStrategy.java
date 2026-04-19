package com.iae.evaluation.strategies;

public class TrimLinesStrategy implements ComparisonStrategy {

    private static final String regex = "\\r?\\n";

    @Override
    public boolean compare(String actual, String expected) {
        if (actual == null && expected == null) {
            return true;
        }

        if (actual == null || expected == null) {
            return false;
        }

        String[] actualLines = actual.split(regex);
        String[] expectedLines = expected.split(regex);

        if (actualLines.length != expectedLines.length) {
            return false;
        }

        for (int i = 0; i < actualLines.length; i++) {
            if (!actualLines[i].trim().equals(expectedLines[i].trim())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getStrategyName() {
        return "Trim Lines";
    }
}
