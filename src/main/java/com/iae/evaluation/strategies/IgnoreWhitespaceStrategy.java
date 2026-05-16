package com.iae.evaluation.strategies;

public class IgnoreWhitespaceStrategy implements ComparisonStrategy {
    private static final String regex = "\\s+";

    @Override
    public boolean compare(String actual, String expected) {
        if (actual == null && expected == null) {
            return true;
        }

        if (actual == null || expected == null) {
            return false;
        }

        return normalize(actual).equals(normalize(expected));
    }

    private String normalize(String text) {
        return text.replaceAll(regex, " ").strip();
    }

    @Override
    public String getStrategyName() {
        return "Ignore Whitespace";
    }
}
