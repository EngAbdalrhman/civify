package com.app.civify.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Type-safe configuration for Application URL properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    // Maps to app.frontend.base-url
    // Nested structure in properties file: app.frontend.base-url
    // Spring Boot Relaxed Binding: frontend-base-url or frontend.base-url -> getFrontend().setBaseUrl()
    @NotNull
    private Frontend frontend = new Frontend();
    @NotNull
    private Backend backend = new Backend();

    @Data
    public static class Frontend {
        @NotBlank
        private String baseUrl;
    }

    @Data
    public static class Backend {
        @NotBlank
        private String baseUrl = "http://localhost:8077"; // Default
    }
}
