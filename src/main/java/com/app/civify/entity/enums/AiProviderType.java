package com.app.civify.entity.enums;

public enum AiProviderType {
    OPENAI,
    ANTHROPIC,
    OPENROUTER,
    GEMINI,
    VERTEX;

    public String toProviderName() {
        return name().toLowerCase();
    }

    public static AiProviderType fromProviderName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Provider name is required");
        }
        return valueOf(name.trim().toUpperCase());
    }
}
