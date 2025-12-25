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
        log.info("ApiKeyFilter: Checking path: {}", path);

        // Use contains to handle context paths (e.g. /apis/api/v1/external)
        if (!path.contains("/external")) {
            log.info("ApiKeyFilter: Skipping path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        log.info("ApiKeyFilter: API Key Header present: {}", (apiKeyHeader != null && !apiKeyHeader.isBlank()));

        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            log.warn("ApiKeyFilter: Missing API Key");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing API Key");
            return;
        }

        try {
            ApiKey apiKey = apiKeyService.validateKey(apiKeyHeader);
            
            if (apiKey == null) {
                log.warn("ApiKeyFilter: Invalid API Key");
                apiKeyService.logUsage(null, null, path, request.getMethod(), HttpServletResponse.SC_UNAUTHORIZED, false, getClientIp(request), "Invalid or Inactive API Key");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or Inactive API Key");
                return;
            }

            log.info("ApiKeyFilter: Valid API Key for user: {}", apiKey.getUser().getEmail());

            // Set authentication in context
            UserPrincipal principal = new UserPrincipal(apiKey.getUser());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Proceed with request
            filterChain.doFilter(request, response);
            
            // Log success after request is processed
            log.info("ApiKeyFilter: Logging usage for user: {}", apiKey.getUser().getEmail());
            apiKeyService.logUsage(apiKey.getUser(), apiKey, path, request.getMethod(), response.getStatus(), true, getClientIp(request), null);
            log.info("ApiKeyFilter: Usage logged successfully");

        } catch (Exception e) {
            log.error("API Key authentication error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Internal Server Error\", \"message\": \"" + e.getMessage() + "\"}");
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
