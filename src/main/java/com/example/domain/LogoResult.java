package com.example.domain;

import java.util.List;

public record LogoResult(boolean passed, List<String> findings) {}