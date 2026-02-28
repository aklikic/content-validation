package com.example.domain;

public record PushRequest(String contentId, String target, String payload) {}