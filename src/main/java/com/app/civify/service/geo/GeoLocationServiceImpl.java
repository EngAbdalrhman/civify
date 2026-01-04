package com.app.civify.service.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of GeoLocationService using ip-api.com (free tier: 45 req/min)
 * Results are cached to reduce API calls
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeoLocationServiceImpl implements GeoLocationService {

    private final RestTemplate restTemplate;
    
    // ip-api.com free tier - no API key required
    // Fields: countryCode, country, regionName, city, timezone, currency
    private static final String IP_API_URL = "http://ip-api.com/json/%s?fields=status,message,countryCode,country,regionName,city,timezone,currency,query";
    
    // Fallback: ipapi.co (free tier: 30k/month)
    private static final String IPAPI_CO_URL = "https://ipapi.co/%s/json/";

    @Override
    @Cacheable(value = "geoLocation", key = "#ipAddress", unless = "#result.success == false")
    public GeoLocationResult getLocation(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return GeoLocationResult.failed(ipAddress, "IP address is null or empty");
        }
        
        // Skip localhost/private IPs
        if (isPrivateIp(ipAddress)) {
            log.debug("Skipping geolocation for private IP: {}", ipAddress);
            return GeoLocationResult.builder()
                    .ip(ipAddress)
                    .success(true)
                    .countryCode("XX") // Unknown/Private
                    .countryName("Private Network")
                    .build();
        }
        
        try {
            return lookupWithIpApi(ipAddress);
        } catch (Exception e) {
            log.warn("ip-api.com lookup failed for {}: {}", ipAddress, e.getMessage());
            try {
                return lookupWithIpapiCo(ipAddress);
            } catch (Exception e2) {
                log.error("All geolocation lookups failed for {}", ipAddress, e2);
                return GeoLocationResult.failed(ipAddress, "All geolocation services failed");
            }
        }
    }

    @Override
    public GeoLocationResult getLocationFromRequest(HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        return getLocation(clientIp);
    }

    @Override
    public String getCountryCode(String ipAddress) {
        GeoLocationResult result = getLocation(ipAddress);
        return result.isSuccess() ? result.getCountryCode() : null;
    }

    @Override
    public String extractClientIp(HttpServletRequest request) {
        // Check common proxy headers
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Lookup using ip-api.com (primary)
     */
    private GeoLocationResult lookupWithIpApi(String ipAddress) {
        String url = String.format(IP_API_URL, ipAddress);
        IpApiResponse response = restTemplate.getForObject(url, IpApiResponse.class);
        
        if (response == null) {
            throw new RuntimeException("Null response from ip-api.com");
        }
        
        if (!"success".equals(response.getStatus())) {
            throw new RuntimeException("ip-api.com error: " + response.getMessage());
        }
        
        return GeoLocationResult.builder()
                .ip(response.getQuery())
                .countryCode(response.getCountryCode())
                .countryName(response.getCountry())
                .region(response.getRegionName())
                .city(response.getCity())
                .timezone(response.getTimezone())
                .currency(response.getCurrency())
                .success(true)
                .build();
    }
    
    /**
     * Fallback lookup using ipapi.co
     */
    private GeoLocationResult lookupWithIpapiCo(String ipAddress) {
        String url = String.format(IPAPI_CO_URL, ipAddress);
        IpapiCoResponse response = restTemplate.getForObject(url, IpapiCoResponse.class);
        
        if (response == null) {
            throw new RuntimeException("Null response from ipapi.co");
        }
        
        if (response.getError() != null && response.getError()) {
            throw new RuntimeException("ipapi.co error: " + response.getReason());
        }
        
        return GeoLocationResult.builder()
                .ip(response.getIp())
                .countryCode(response.getCountryCode())
                .countryName(response.getCountryName())
                .region(response.getRegion())
                .city(response.getCity())
                .timezone(response.getTimezone())
                .currency(response.getCurrency())
                .success(true)
                .build();
    }
    
    /**
     * Check if IP is private/localhost
     */
    private boolean isPrivateIp(String ip) {
        if (ip == null) return true;
        return ip.startsWith("10.") ||
               ip.startsWith("172.16.") || ip.startsWith("172.17.") || ip.startsWith("172.18.") ||
               ip.startsWith("172.19.") || ip.startsWith("172.20.") || ip.startsWith("172.21.") ||
               ip.startsWith("172.22.") || ip.startsWith("172.23.") || ip.startsWith("172.24.") ||
               ip.startsWith("172.25.") || ip.startsWith("172.26.") || ip.startsWith("172.27.") ||
               ip.startsWith("172.28.") || ip.startsWith("172.29.") || ip.startsWith("172.30.") ||
               ip.startsWith("172.31.") ||
               ip.startsWith("192.168.") ||
               ip.equals("127.0.0.1") ||
               ip.equals("0:0:0:0:0:0:0:1") ||
               ip.equals("::1");
    }
    
    // Response DTOs for external APIs
    
    @Data
    private static class IpApiResponse {
        private String status;
        private String message;
        private String countryCode;
        private String country;
        private String regionName;
        private String city;
        private String timezone;
        private String currency;
        private String query; // The IP address
    }
    
    @Data
    private static class IpapiCoResponse {
        private String ip;
        @JsonProperty("country_code")
        private String countryCode;
        @JsonProperty("country_name")
        private String countryName;
        private String region;
        private String city;
        private String timezone;
        private String currency;
        private Boolean error;
        private String reason;
    }
}
