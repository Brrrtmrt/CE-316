package com.iae.evaluation.strategies;

public class TrimLinesStrategy implements ComparisonStrategy {

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
        String s = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = s.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.stripTrailing()).append("\n");
        }
        return sb.toString().strip();
    }

    @Override
    public String getStrategyName() {
        return "Trim Lines";
    }
}
