package com.example.domain;

import java.util.Map;

public record EnterpriseRequest(String content, Map<String, String> metadata) {}