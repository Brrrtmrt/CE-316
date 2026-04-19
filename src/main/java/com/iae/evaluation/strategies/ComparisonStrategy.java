package com.iae.evaluation.strategies;

public interface ComparisonStrategy {

    boolean compare(String actual, String expected);

    String getStrategyName();
}
