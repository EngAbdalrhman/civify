package com.app.civify.service;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.cache.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/** Bridges the existing Civify web login to MCP OAuth without exposing an API key to the browser. */
@Service
@Slf4j
public class McpAccountLinkService {
    private static final String PREFIX = "mcp:link:";
    private static final Set<String> SCOPES = Set.of("cv:read", "cv:parse", "cv:tailor", "cv:export",
            "ats:score", "pii:mask", "jobs:scrape", "apps:read", "apps:write",
            "billing:read", "billing:purchase", "account:read");
    private final CacheService cache;
    private final ApiKeyService keys;
    private final UserRepository users;
    private final ObjectMapper mapper;
    private final URI gateway;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public McpAccountLinkService(CacheService cache, ApiKeyService keys, UserRepository users, ObjectMapper mapper,
            @Value("${civify.mcp.origin:https://mcp.civify.cv}") String origin) {
        this.cache = cache; this.keys = keys; this.users = users; this.mapper = mapper;
        gateway = URI.create(origin);
        if (gateway.getHost() == null || gateway.getUserInfo() != null || gateway.getQuery() != null ||
                gateway.getFragment() != null || !(gateway.getPath().isEmpty() || gateway.getPath().equals("/")) ||
                !("https".equals(gateway.getScheme()) || ("http".equals(gateway.getScheme()) &&
                        Set.of("localhost", "127.0.0.1").contains(gateway.getHost())))) {
            throw new IllegalArgumentException("civify.mcp.origin must be an HTTPS origin (or loopback HTTP)");
        }
    }

    public record LinkRequest(String transaction, String clientName, String redirectUri, String challenge, long expiresAt) {}
    private record Pending(Long userId, String transaction, String challenge, String clientName, long expiresAt) {}

    public LinkRequest request(String transaction) {
        requireOpaque(transaction);
        try {
            // The origin is deployment configuration, never browser input. Do not follow redirects.
            var response = http.send(HttpRequest.newBuilder(gateway.resolve("/oauth/transactions/" + transaction))
                    .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 16_384) throw invalid();
            var data = mapper.readValue(response.body(), LinkRequest.class);
            requireOpaque(data.challenge());
            if (!transaction.equals(data.transaction()) || data.expiresAt() <= System.currentTimeMillis() ||
                    data.clientName() == null || data.clientName().length() > 256 || data.redirectUri() == null) throw invalid();
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account linking unavailable. Reconnect from your AI client.");
        } catch (Exception e) {
            throw invalid();
        }
    }

    public Map<String, Object> describe(String transaction, User user) {
        var request = request(transaction);
        return Map.of("clientName", request.clientName(), "redirectUri", request.redirectUri(), "expiresAt", request.expiresAt(),
                "account", user.getEmail(), "cancelUrl", callback(transaction) + "&error=access_denied");
    }

    public Map<String, String> authorize(String transaction, User user) {
        if (!user.isEnabled() || !user.isAccountNonLocked()) throw invalid();
        var request = request(transaction);
        Long count = cache.increment(PREFIX + "rate:" + user.getId());
        if (count != null && count == 1) cache.expire(PREFIX + "rate:" + user.getId(), Duration.ofHours(1));
        if (count == null || count > 30) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many connection attempts. Try again later.");
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long expires = Math.min(request.expiresAt(), System.currentTimeMillis() + 60_000);
        try {
            cache.set(PREFIX + digest(code), mapper.writeValueAsString(new Pending(user.getId(), transaction,
                    request.challenge(), request.clientName(), expires)), Duration.ofMillis(expires - System.currentTimeMillis()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account linking unavailable. Please retry.");
        }
        log.info("MCP account link approved userId={}", user.getId());
        return Map.of("redirectUrl", callback(transaction) + "&code=" + code);
    }

    public Map<String, Object> exchange(String code, String verifier, String transaction) {
        requireOpaque(code); requireOpaque(verifier); requireOpaque(transaction);
        try {
            String cacheKey = PREFIX + digest(code);
            String value = cache.get(cacheKey);
            if (value == null) throw invalid();
            Pending pending = mapper.readValue(value, Pending.class);
            if (pending.expiresAt() <= System.currentTimeMillis() || !transaction.equals(pending.transaction()) ||
                    !MessageDigest.isEqual(digest(verifier).getBytes(StandardCharsets.US_ASCII),
                            pending.challenge().getBytes(StandardCharsets.US_ASCII))) throw invalid();
            // Atomic consume after proof validation prevents replay and wrong-verifier denial of service.
            if (!value.equals(cache.getAndDelete(cacheKey))) throw invalid();
            User user = users.findById(pending.userId()).orElseThrow(McpAccountLinkService::invalid);
            if (!user.isEnabled() || !user.isAccountNonLocked()) throw invalid();
            LocalDateTime expires = LocalDateTime.now().plusDays(30);
            String key = keys.createScopedApiKey(user, "MCP: " + pending.clientName().substring(0, Math.min(80, pending.clientName().length())),
                    "Connected through Civify sign-in. Revoke this key to disconnect the AI client.", SCOPES, expires);
            log.info("MCP account link exchanged userId={}", user.getId());
            return Map.of("apiKey", key, "expiresAt", expires.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        } catch (Exception e) {
            // No codes, keys, JWTs, email addresses or upstream response bodies in logs/errors.
            log.info("MCP account link exchange rejected");
            throw invalid();
        }
    }

    private String callback(String transaction) { return gateway.resolve("/oauth/complete").toString() + "?transaction=" + transaction; }
    private static String digest(String input) {
        try { return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.US_ASCII))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static void requireOpaque(String value) { if (value == null || !value.matches("[A-Za-z0-9_-]{43}")) throw invalid(); }
    private static ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired account link. Reconnect from your AI client."); }
}
