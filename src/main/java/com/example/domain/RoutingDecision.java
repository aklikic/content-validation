package com.example.domain;

public record RoutingDecision(String target, boolean compliant, String reason) {}