package com.app.civify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for payment gateways and their supported currencies
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
@Data
public class PaymentGatewayConfig {

    private GatewayConfig fawaterak = new GatewayConfig();
    private GatewayConfig paymob = new GatewayConfig();

    @Data
    public static class GatewayConfig {
        private String baseUrl;
        private String apiKey;
        private String providerKey;
        private List<String> currencies = new ArrayList<>();
        private boolean enabled = true;
    }

    /**
     * Check if a gateway supports a specific currency
     */
    public boolean isGatewayAvailableForCurrency(String gateway, String currency) {
        if (gateway == null || currency == null) return false;
        
        return switch (gateway.toLowerCase()) {
            case "fawaterak" -> fawaterak.isEnabled() && fawaterak.getCurrencies().stream()
                    .anyMatch(c -> c.equalsIgnoreCase(currency));
            case "paymob" -> paymob.isEnabled() && paymob.getCurrencies().stream()
                    .anyMatch(c -> c.equalsIgnoreCase(currency));
            default -> false;
        };
    }

    /**
     * Get list of payment gateways available for a currency
     */
    public List<String> getAvailableGatewaysForCurrency(String currency) {
        List<String> gateways = new ArrayList<>();
        
        if (isGatewayAvailableForCurrency("fawaterak", currency)) {
            gateways.add("fawaterak");
        }
        if (isGatewayAvailableForCurrency("paymob", currency)) {
            gateways.add("paymob");
        }
        
        return gateways;
    }
}
