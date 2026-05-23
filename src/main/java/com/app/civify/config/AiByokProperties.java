package com.app.civify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "civify.byok")
public class AiByokProperties {

    /** Master switch — when false, AiServiceImpl uses legacy platform-only path. */
    private boolean enabled = false;

    private String encryptionKey = "";

    /** Providers allowed on the platform path (not BYOK). */
    private Set<String> platformProvidersAllowed = Set.of("gemini", "vertex");
}
