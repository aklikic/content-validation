package com.example.domain;

import java.util.Map;

public record ContentRequest(String contentId, String payload, Map<String, String> metadata) {}