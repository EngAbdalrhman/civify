package com.app.civify.service.payment;

import com.maxmind.db.CHMCache;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    @Value("${payment.gateway.region-strict:true}")
    private boolean regionStrict;

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
            // CHMCache adds an in-memory cache for decoded nodes (~2 MB), recommended by MaxMind
            // for any reader that handles repeated lookups. The reader itself is reused as a
            // Spring singleton, so this gives constant-time hits for common IPs.
            this.geoReader = new DatabaseReader.Builder(dbFile)
                    .withCache(new CHMCache())
                    .build();
            log.info("Successfully loaded MaxMind GeoIP2 database from {} (with CHMCache).", dbPath);
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

    /**
     * Resolves the country code for the request's client IP.
     * Returns null if undetectable (proxy, local, or MaxMind missing).
     */
    public String resolveCountryCode(HttpServletRequest request) {
        String ip = extractClientIp(request);
        if (ip == null || ip.isBlank() || isLocalIp(ip)) {
            return null;
        }
        return lookupCountry(ip);
    }

    /**
     * Returns the gateways permitted for this request's region.
     * - MENA region or undetectable → MENA gateways (paymob, fawaterak)
     * - Non-MENA region → global gateway (dodo)
     * If region-strict mode is OFF, returns all enabled gateways.
     */
    public List<String> filterGatewaysForRegion(HttpServletRequest request, List<String> enabledGateways) {
        if (enabledGateways == null || enabledGateways.isEmpty()) {
            return Collections.emptyList();
        }
        if (!regionStrict) {
            return enabledGateways;
        }
        if (!dodoEnabled) {
            // Dodo off: just expose MENA gateways that are enabled
            return enabledGateways.stream()
                    .filter(g -> !"dodo".equalsIgnoreCase(g))
                    .toList();
        }

        String country = resolveCountryCode(request);
        boolean isMena = country != null && MENA_COUNTRY_CODES.contains(country.toUpperCase());
        boolean isNonMena = country != null && !isMena;

        List<String> filtered = new ArrayList<>();
        for (String g : enabledGateways) {
            String gLower = g.toLowerCase();
            if ("dodo".equals(gLower)) {
                if (isNonMena) {
                    filtered.add(g);
                }
            } else {
                // paymob, fawaterak: show to MENA or when country is unknown (safe default)
                if (!isNonMena) {
                    filtered.add(g);
                }
            }
        }
        // Safety: never return an empty list if we have gateways enabled
        if (filtered.isEmpty()) {
            log.warn("Region filter produced empty gateway list for country {}. Returning all enabled gateways.", country);
            return enabledGateways;
        }
        return filtered;
    }

    public String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // Cloudflare
        String cf = request.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) {
            return cf.trim();
        }
        // Generic / nginx style
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        // Stainless / dodo style
        String trueClient = request.getHeader("True-Client-IP");
        if (trueClient != null && !trueClient.isBlank()) {
            return trueClient.trim();
        }
        // Standard XFF: first IP is the original client
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
            // geoip2 5.x: model classes are Java records; use record-style accessors.
            if (response != null && response.country() != null) {
                return response.country().isoCode();
            }
        } catch (Exception e) {
            log.warn("MaxMind lookup failed for IP: {}. Message: {}", ip, e.getMessage());
        }
        return null;
    }
}
