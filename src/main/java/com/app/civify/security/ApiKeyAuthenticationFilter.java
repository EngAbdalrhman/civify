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

        // Allow CORS preflight requests to pass through without API key validation
        // Preflight OPTIONS requests don't include custom headers like X-API-KEY
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.info("ApiKeyFilter: Allowing CORS preflight OPTIONS request for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        boolean isExternal = path.contains("/external");

        // If not an external endpoint and no API key header is provided, pass through to other filters (e.g. JWT)
        if (!isExternal && (apiKeyHeader == null || apiKeyHeader.isBlank())) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("ApiKeyFilter: API Key Header present: {}", (apiKeyHeader != null && !apiKeyHeader.isBlank()));

        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            log.warn("ApiKeyFilter: Missing API Key for external endpoint: {}", path);
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

            log.info("ApiKeyFilter: Valid API Key for user: {}", apiKey.getUser().getEmail());

            // Set authentication in context
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

            log.info("ApiKeyFilter: Logging usage for user: {}. Status: {}, Success: {}", 
                    apiKey.getUser().getEmail(), status, success);
            
            apiKeyService.logUsage(apiKey.getUser(), apiKey, path, request.getMethod(), 
                    status, success, getClientIp(request), errorMessage);
            
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
