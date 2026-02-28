package com.example.domain;

public record AggregatedResult(boolean overallPassed, double confidence, String summary) {}