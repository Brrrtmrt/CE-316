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

        String normalizedActual = actual.replaceAll(regex, "");
        String normalizedExpected = expected.replaceAll(regex, "");

        return normalizedActual.equals(normalizedExpected);
    }

    @Override
    public String getStrategyName() {
        return "Ignore Whitespace";
    }
}
