package com.app.civify.service.geo;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for IP-based geolocation
 */
public interface GeoLocationService {
    
    /**
     * Get geolocation information for an IP address
     * @param ipAddress The IP address to lookup
     * @return GeoLocationResult with country code and other location data
     */
    GeoLocationResult getLocation(String ipAddress);
    
    /**
     * Get geolocation information from HTTP request
     * Automatically extracts IP from X-Forwarded-For or remote address
     * @param request The HTTP request
     * @return GeoLocationResult with country code and other location data
     */
    GeoLocationResult getLocationFromRequest(HttpServletRequest request);
    
    /**
     * Get just the country code for an IP address
     * @param ipAddress The IP address to lookup
     * @return ISO 3166-1 alpha-2 country code (e.g., "US", "EG") or null if lookup fails
     */
    String getCountryCode(String ipAddress);
    
    /**
     * Extract client IP address from HTTP request
     * Handles X-Forwarded-For header for proxied requests
     * @param request The HTTP request
     * @return Client IP address
     */
    String extractClientIp(HttpServletRequest request);
}
