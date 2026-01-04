package com.app.civify.service.geo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from IP geolocation lookup
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationResult {
    
    private String ip;
    private String countryCode;    // ISO 3166-1 alpha-2 (e.g., "US", "EG", "SA")
    private String countryName;    // Full country name (e.g., "United States", "Egypt")
    private String region;         // Region/State
    private String city;           // City name
    private String timezone;       // Timezone (e.g., "America/New_York")
    private String currency;       // Currency code (e.g., "USD", "EGP")
    private boolean success;       // Whether lookup succeeded
    private String errorMessage;   // Error message if lookup failed
    
    /**
     * Create a failed result
     */
    public static GeoLocationResult failed(String ip, String errorMessage) {
        return GeoLocationResult.builder()
                .ip(ip)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Check if this is an Egyptian IP (for pricing purposes)
     */
    public boolean isEgypt() {
        return "EG".equalsIgnoreCase(countryCode);
    }
    
    /**
     * Check if this is a Middle East/North Africa region
     */
    public boolean isMENA() {
        if (countryCode == null) return false;
        return switch (countryCode.toUpperCase()) {
            case "EG", "SA", "AE", "KW", "QA", "BH", "OM", "JO", "LB", "IQ", "SY", "PS", "YE",
                 "MA", "DZ", "TN", "LY", "SD" -> true;
            default -> false;
        };
    }
}
