package com.app.civify.security;

import com.app.civify.entity.security.ApiKey;
import com.app.civify.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private static final String API_KEY_HEADER = "X-API-KEY";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.debug("ApiKeyFilter: Checking path: {}", path);

        // Only authenticate routes handled by the versioned external API. A
        // segment match avoids capturing unrelated routes that happen to use
        // the word "external" in a resource name.
        if (!path.matches("(?:.*/)?v1/external(?:/.*)?")) {
            log.debug("ApiKeyFilter: Skipping path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Allow CORS preflight requests to pass through without API key validation
        // Preflight OPTIONS requests don't include custom headers like X-API-KEY
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("ApiKeyFilter: Allowing CORS preflight OPTIONS request for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Allow public external endpoints to pass without API key
        if (path.endsWith("/v1/external/pay-per-cv/pricing") || path.endsWith("/v1/external/cvs/scrape-jd")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        log.debug("ApiKeyFilter: API Key Header present: {}", (apiKeyHeader != null && !apiKeyHeader.isBlank()));

        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            log.warn("ApiKeyFilter: Missing API Key");
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "API_KEY_MISSING", "Missing API Key");
            return;
        }

        try {
            ApiKey apiKey = apiKeyService.validateKey(apiKeyHeader);
            
            if (apiKey == null) {
                log.warn("ApiKeyFilter: Invalid API Key");
                logUsageSafely(null, path, request.getMethod(), HttpServletResponse.SC_UNAUTHORIZED, false, getClientIp(request), "Invalid or Inactive API Key");
                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "API_KEY_INVALID", "Invalid or inactive API key");
                return;
            }

            if (apiKey.isExpired()) {
                log.warn("ApiKeyFilter: Expired API Key");
                logUsageSafely(apiKey, path, request.getMethod(), HttpServletResponse.SC_UNAUTHORIZED, false, getClientIp(request), "API Key Expired");
                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "API_KEY_EXPIRED", "Account access has expired. Reconnect Civify in your MCP client, or replace the key in your developer API-key settings.");
                return;
            }

            // Check if user has API access (either via permission or plan)
            boolean hasApiAccess = false;
            
            // 1. Check permissions
            if (apiKey.getUser().getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("API_ACCESS"))) {
                hasApiAccess = true;
            }
            
            // 2. Check active plan if not already granted via permission
            if (!hasApiAccess && apiKey.getUser().getSubscriptions() != null) {
                hasApiAccess = apiKey.getUser().getSubscriptions().stream()
                        .filter(sub -> sub.isActive())
                        .anyMatch(sub -> Boolean.TRUE.equals(sub.getPlan().getHasApiAccess()));
            }
            
            // if (!hasApiAccess) {
            //     log.warn("ApiKeyFilter: User {} does not have API access", apiKey.getUser().getEmail());
            //     apiKeyService.logUsage(apiKey.getUser(), apiKey, path, request.getMethod(), HttpServletResponse.SC_FORBIDDEN, false, getClientIp(request), "API Access Denied - Plan upgrade required");
            //     response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            //     response.getWriter().write("API Access Denied - Your current plan does not support API access");
            //     return;
            // }

            log.debug("ApiKeyFilter: Valid API key");

            // Set authentication in context and attach key to request
            request.setAttribute("CURRENT_API_KEY", apiKey);

            UserPrincipal principal = new UserPrincipal(apiKey.getUser());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            filterChain.doFilter(request, response);
            
            int status = response.getStatus();
            boolean success = status >= 200 && status < 400;
            
            String errorMessage = (String) request.getAttribute("API_ERROR_MESSAGE");
            if (!success && errorMessage == null) {
                errorMessage = "HTTP Error " + status;
            }

            log.debug("ApiKeyFilter: Logging usage. Status: {}, Success: {}", status, success);
            
            logUsageSafely(apiKey, path, request.getMethod(), status, success, getClientIp(request), errorMessage);
            
            log.debug("ApiKeyFilter: Usage logged successfully");

        } catch (Exception e) {
            log.error("API Key authentication error", e);
            if (!response.isCommitted()) {
                writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "API_KEY_AUTHENTICATION_ERROR", "Unable to authenticate API key");
            }
        }
    }

    private void writeJsonError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"error\":\"%s\",\"message\":\"%s\"}", error, message));
    }

    private void logUsageSafely(ApiKey apiKey, String path, String method, int status, boolean success,
                                String clientIp, String errorMessage) {
        try {
            apiKeyService.logUsage(apiKey != null ? apiKey.getUser() : null, apiKey, path, method,
                    status, success, clientIp, errorMessage);
        } catch (Exception e) {
            // Usage telemetry must never change the outcome of an API request.
            log.error("Failed to record API-key usage for path: {}", path, e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
