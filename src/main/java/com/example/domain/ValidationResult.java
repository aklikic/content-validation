package com.example.domain;

import java.util.List;

public record ValidationResult(String agentId, boolean passed, List<String> issues) {}