package com.app.civify.service;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.cache.InMemoryCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class McpAccountLinkServiceTest {
    private final String transaction = "t".repeat(43), verifier = "v".repeat(43);
    private InMemoryCacheService cache;
    private ApiKeyService keys;
    private UserRepository users;
    private User user;
    private McpAccountLinkService service;
    private String challenge;

    @BeforeEach void setup() throws Exception {
        cache = new InMemoryCacheService(); keys = mock(ApiKeyService.class); users = mock(UserRepository.class);
        user = mock(User.class); when(user.getId()).thenReturn(7L); when(user.getEmail()).thenReturn("fixture@example.com");
        when(user.isEnabled()).thenReturn(true); when(user.isAccountNonLocked()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(keys.createScopedApiKey(eq(user), anyString(), anyString(), anySet(), any())).thenReturn("cv-fy-fixture");
        challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        service = spy(new McpAccountLinkService(cache, keys, users, new ObjectMapper(), "https://mcp.example.com"));
        doReturn(new McpAccountLinkService.LinkRequest(transaction, "Fixture client", "https://client.example/callback", challenge,
                System.currentTimeMillis() + 600_000)).when(service).request(transaction);
    }
    private String issueCode() {
        String redirect = service.authorize(transaction, user).get("redirectUrl");
        assertTrue(redirect.startsWith("https://mcp.example.com/oauth/complete?transaction="));
        assertFalse(redirect.contains("cv-fy"));
        return redirect.substring(redirect.indexOf("&code=") + 6);
    }
    @Test void issuesCredentialOnlyDuringVerifiedExchangeAndRejectsReplay() {
        String code = issueCode(); verifyNoInteractions(keys);
        var result = service.exchange(code, verifier, transaction);
        assertEquals("cv-fy-fixture", result.get("apiKey"));
        assertTrue((long) result.get("expiresAt") > System.currentTimeMillis());
        verify(keys).createScopedApiKey(eq(user), eq("MCP: Fixture client"), anyString(),
                eq(Set.of("cv:read", "cv:parse", "cv:tailor", "cv:export", "ats:score", "pii:mask",
                        "jobs:scrape", "apps:read", "apps:write", "billing:read", "billing:purchase", "account:read")), notNull());
        assertThrows(ResponseStatusException.class, () -> service.exchange(code, verifier, transaction));
    }
    @Test void wrongProofOrTransactionDoesNotConsumeTheValidCode() {
        String code = issueCode();
        assertThrows(ResponseStatusException.class, () -> service.exchange(code, "x".repeat(43), transaction));
        assertThrows(ResponseStatusException.class, () -> service.exchange(code, verifier, "x".repeat(43)));
        assertEquals("cv-fy-fixture", service.exchange(code, verifier, transaction).get("apiKey"));
    }
    @Test void disabledAccountCannotLinkOrFinishAnExistingLink() {
        String code = issueCode(); when(user.isEnabled()).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> service.authorize(transaction, user));
        assertThrows(ResponseStatusException.class, () -> service.exchange(code, verifier, transaction));
        verifyNoInteractions(keys);
    }
    @Test void expiredOrMissingCodesNeverCreateCredentials() throws Exception {
        String code = issueCode();
        String key = "mcp:link:" + Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.US_ASCII)));
        cache.set(key, cache.get(key).replaceAll("\"expiresAt\":\\d+", "\"expiresAt\":1"), Duration.ofMinutes(1));
        assertThrows(ResponseStatusException.class, () -> service.exchange(code, verifier, transaction));
        assertThrows(ResponseStatusException.class, () -> service.exchange("z".repeat(43), verifier, transaction));
        verifyNoInteractions(keys);
    }
    @Test void atomicConsumeAllowsOnlyOneConcurrentExchange() {
        String code = issueCode(); AtomicInteger success = new AtomicInteger();
        IntStream.range(0, 16).parallel().forEach(i -> {
            try { service.exchange(code, verifier, transaction); success.incrementAndGet(); } catch (ResponseStatusException ignored) {}
        });
        assertEquals(1, success.get());
        verify(keys, times(1)).createScopedApiKey(any(), anyString(), anyString(), anySet(), any());
    }
    @Test void consentShowsAccountAndActualRedirectAndNeverContainsCredential() {
        var result = service.describe(transaction, user);
        assertEquals("fixture@example.com", result.get("account"));
        assertEquals("https://client.example/callback", result.get("redirectUri"));
        assertTrue(result.get("cancelUrl").toString().endsWith("&error=access_denied"));
        assertFalse(result.containsKey("apiKey")); verifyNoInteractions(keys);
    }
    @Test void requestLookupIsBoundToConfiguredGatewayAndRejectsRedirects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger hits = new AtomicInteger();
        server.createContext("/oauth/transactions/", exchange -> {
            hits.incrementAndGet();
            exchange.getResponseHeaders().set("Location", "https://example.invalid/private");
            exchange.sendResponseHeaders(302, -1); exchange.close();
        });
        server.start();
        try {
            var real = new McpAccountLinkService(cache, keys, users, new ObjectMapper(), "http://127.0.0.1:" + server.getAddress().getPort());
            assertThrows(ResponseStatusException.class, () -> real.request("../../private"));
            assertEquals(0, hits.get());
            assertThrows(ResponseStatusException.class, () -> real.request(transaction));
            assertEquals(1, hits.get());
        } finally { server.stop(0); }
    }
    @Test void rejectsUntrustedGatewayConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new McpAccountLinkService(cache, keys, users, new ObjectMapper(), "http://example.com"));
        assertThrows(IllegalArgumentException.class, () -> new McpAccountLinkService(cache, keys, users, new ObjectMapper(), "https://user:pass@example.com"));
    }
}
