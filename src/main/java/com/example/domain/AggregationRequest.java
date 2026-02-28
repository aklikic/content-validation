package com.example.domain;

import java.util.List;

public record AggregationRequest(String contentId, List<ValidationResult> results) {}