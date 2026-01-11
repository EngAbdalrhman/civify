package com.app.civify.security;

import com.app.civify.entity.User;
import com.app.civify.entity.enums.FeatureType;
import com.app.civify.entity.security.ApiKey;
import com.app.civify.service.ApiKeyService;
import com.app.civify.service.subscription.SubscriptionService;
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
    private final SubscriptionService subscriptionService;
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

            User user = apiKey.getUser();
            log.info("ApiKeyFilter: Valid API Key for user: {}", user.getEmail());

            // Check if user's subscription has API_ACCESS feature
            if (!subscriptionService.hasFeatureAccess(user.getId(), FeatureType.API_ACCESS)) {
                log.warn("ApiKeyFilter: User {} lacks API_ACCESS feature in their subscription", user.getEmail());
                
                apiKeyService.logUsage(user, apiKey, path, request.getMethod(), 
                    HttpServletResponse.SC_FORBIDDEN, false, getClientIp(request), 
                    "API access requires a subscription with API_ACCESS feature enabled");
                
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"API access requires a paid subscription with API access enabled\"}");
                return;
            }

            // Set authentication in context
            UserPrincipal principal = new UserPrincipal(user);
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
