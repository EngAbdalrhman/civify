package com.app.civify.service.payment;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;

/**
 * Resolves the appropriate payment gateway based on the client's geographic IP region.
 * Uses MaxMind GeoLite2 Country database for extremely fast offline lookups.
 * Safe fallback mechanisms ensure payment flows never block on MaxMind failures.
 */
@Service
@Slf4j
public class GatewayRegionResolver {

    private static final Set<String> MENA_COUNTRY_CODES = Set.of(
            "EG", "SA", "AE", "KW", "QA", "BH", "OM", "JO",
            "LB", "IQ", "PS", "LY", "TN", "DZ", "MA", "SD", "YE", "SY"
    );

    @Value("${payment.gateway.dodo.enabled:false}")
    private boolean dodoEnabled;

    @Value("${payment.gateway.routing.strategy:geo}")
    private String routingStrategy;

    @Value("${payment.geo.maxmind.db-path:./data/GeoLite2-Country.mmdb}")
    private String dbPath;

    @Value("${payment.gateway.default:paymob}")
    private String defaultGateway;

    private DatabaseReader geoReader;

    @PostConstruct
    public void init() {
        if (!"geo".equalsIgnoreCase(routingStrategy)) {
            log.info("Geo-IP routing strategy is not enabled (strategy: {}). MaxMind DB will not be loaded.", routingStrategy);
            return;
        }

        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            log.warn("MaxMind GeoIP2 database file not found at path: {}. Dynamic geo-routing will fall back to default gateways.", dbPath);
            return;
        }

        try {
            this.geoReader = new DatabaseReader.Builder(dbFile).build();
            log.info("Successfully loaded MaxMind GeoIP2 database from {}.", dbPath);
        } catch (IOException e) {
            log.error("Failed to initialize MaxMind GeoIP2 DatabaseReader from path: {}", dbPath, e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (geoReader != null) {
            try {
                geoReader.close();
                log.info("Closed MaxMind GeoIP2 DatabaseReader.");
            } catch (IOException e) {
                log.error("Error closing MaxMind GeoIP2 DatabaseReader", e);
            }
        }
    }

    /**
     * Resolves the best gateway based on the caller's IP address.
     * If Geo routing is disabled, Dodo is disabled, or lookup fails, returns the safe default gateway.
     */
    public String resolveGateway(HttpServletRequest request) {
        if (!dodoEnabled) {
            log.debug("Dodo Payments is disabled. Routing directly to default gateway: {}", defaultGateway);
            return defaultGateway;
        }

        if (!"geo".equalsIgnoreCase(routingStrategy)) {
            log.debug("Routing strategy is manual or non-geo. Returning default gateway: {}", defaultGateway);
            return defaultGateway;
        }

        String ip = extractClientIp(request);
        if (ip == null || ip.isBlank() || isLocalIp(ip)) {
            log.debug("Local or invalid IP detected ({}). Falling back to default gateway: {}", ip, defaultGateway);
            return defaultGateway;
        }

        String countryCode = lookupCountry(ip);
        if (countryCode == null) {
            log.warn("Could not determine country for IP: {}. Falling back to default gateway: {}", ip, defaultGateway);
            return defaultGateway;
        }

        log.info("Resolved country {} for client IP {}", countryCode, ip);
        if (MENA_COUNTRY_CODES.contains(countryCode.toUpperCase())) {
            log.info("MENA/Egypt region detected. Routing to standard gateway: paymob");
            return "paymob";
        }

        log.info("Non-MENA region detected. Routing to global gateway: dodo");
        return "dodo";
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] ips = xff.split(",");
            return ips[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLocalIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
        } catch (Exception e) {
            log.warn("Failed to parse IP to InetAddress for local IP check: {}, defaulting to true", ip);
            return true;
        }
    }


    private String lookupCountry(String ip) {
        if (geoReader == null) {
            return null;
        }
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = geoReader.country(ipAddress);
            if (response != null && response.getCountry() != null) {
                return response.getCountry().getIsoCode();
            }
        } catch (Exception e) {
            log.warn("MaxMind lookup failed for IP: {}. Message: {}", ip, e.getMessage());
        }
        return null;
    }
}
