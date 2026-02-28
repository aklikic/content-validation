package com.example.domain;

public record RoutingRequest(String contentId, AggregatedResult aggregatedResult, ReviewDecision reviewDecision) {}