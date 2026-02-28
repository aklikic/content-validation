package com.example.domain;

import java.util.List;

public record NLPResult(String callReason, boolean passed, List<String> issues) {}