package com.iae.evaluation.strategies;

public class ExactMatchStrategy implements ComparisonStrategy {

    @Override
    public boolean compare(String actual, String expected) {
        if (actual == null && expected == null) {
            return true;
        }

        if (actual == null || expected == null) {
            return false;
        }

        return actual.equals(expected);
    }

    @Override
    public String getStrategyName() {
        return "Exact Match";
    }
}
