package com.app.civify.config;

import com.dodopayments.api.client.DodoPaymentsClient;
import com.dodopayments.api.client.okhttp.DodoPaymentsOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(name = "payment.gateway.dodo.enabled", havingValue = "true")
public class DodoPaymentsConfig {

    @Value("${payment.dodo.api-key:}")
    private String apiKey;

    @Value("${payment.dodo.webhook-key:}")
    private String webhookKey;

    @Value("${payment.dodo.environment:test_mode}")
    private String environment;

    @PostConstruct
    public void validateConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Dodo Payments API Key (payment.dodo.api-key) is not configured, but Dodo Payments gateway is enabled.");
        }
        if (webhookKey == null || webhookKey.isBlank()) {
            throw new IllegalStateException("Dodo Payments Webhook Key (payment.dodo.webhook-key) is not configured, but Dodo Payments gateway is enabled.");
        }
    }

    @Bean
    public DodoPaymentsClient dodoPaymentsClient() {
        String baseUrl = "test_mode".equalsIgnoreCase(environment)
                ? "https://test.dodopayments.com"
                : "https://live.dodopayments.com";

        return DodoPaymentsOkHttpClient.builder()
                .bearerToken(apiKey)
                .webhookKey(webhookKey)
                .baseUrl(baseUrl)
                .build();
    }
}

