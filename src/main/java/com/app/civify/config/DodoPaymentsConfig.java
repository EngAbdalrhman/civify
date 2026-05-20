package com.app.civify.config;

import com.dodopayments.api.client.DodoPaymentsClient;
import com.dodopayments.api.client.okhttp.DodoPaymentsOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "payment.gateway.dodo.enabled", havingValue = "true")
public class DodoPaymentsConfig {

    @Value("${payment.dodo.api-key:}")
    private String apiKey;

    @Value("${payment.dodo.webhook-key:}")
    private String webhookKey;

    @Value("${payment.dodo.environment:test_mode}")
    private String environment;

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
