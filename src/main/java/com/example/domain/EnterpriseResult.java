package com.example.domain;

import java.util.List;

public record EnterpriseResult(boolean passed, List<String> violations) {}