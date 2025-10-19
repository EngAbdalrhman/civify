package com.app.civify.util;

import com.app.civify.entity.ApiUsageLog;
import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.ApiUsageLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Enumeration;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final ApiUsageLogService apiUsageLogService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUri = uri + (queryString != null ? "?" + queryString : "");
        String remoteAddr = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String requestId = java.util.UUID.randomUUID().toString();

        // Add request ID to response header for tracking
        response.setHeader("X-Request-ID", requestId);

        // Log incoming request with structured format for ELK
        log.info("REQUEST_START | requestId={} | method={} | uri={} | remoteAddr={} | userAgent={} | timestamp={}",
                requestId, method, fullUri, remoteAddr, userAgent, LocalDateTime.now());

        // Get current user if authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        User user = null;
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            username = auth.getName();
            log.info("AUTHENTICATED_USER | username={} | authorities={}", username, auth.getAuthorities());

            // Try to get user entity for logging
            try {
                user = userRepository.findByEmail(username).orElse(null);
            } catch (Exception e) {
                log.warn("Failed to fetch user for logging: {}", username);
            }
        } else {
            log.info("ANONYMOUS_REQUEST | no authentication");
        }

        // Log request headers for debugging (optional, can be disabled in production)
        if (log.isDebugEnabled()) {
            log.debug("REQUEST_HEADERS | uri={}", fullUri);
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // Skip sensitive headers
                if (!headerName.equalsIgnoreCase("authorization")
                        && !headerName.equalsIgnoreCase("cookie")) {
                    log.debug("HEADER | {}: {}", headerName, request.getHeader(headerName));
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            boolean isSuccess = status >= 200 && status < 400;

            // Log response with structured format for ELK
            log.info("REQUEST_END | requestId={} | method={} | uri={} | status={} | duration={}ms | success={} | timestamp={}",
                    requestId, method, fullUri, status, duration, isSuccess, LocalDateTime.now());

            // Create API usage log entry for authenticated requests
            if (user != null && !isExcludedEndpoint(uri)) {
                try {
                    ApiUsageLog logEntry = new ApiUsageLog();
                    logEntry.setUser(user);
                    logEntry.setEndpoint(fullUri);
                    logEntry.setTimestamp(LocalDateTime.now());
                    logEntry.setSuccess(isSuccess);
                    logEntry.setIpAddress(remoteAddr);

                    if (!isSuccess) {
                        logEntry.setErrorMessage("HTTP " + status);
                    }

                    apiUsageLogService.createLog(logEntry);

                    log.debug("API_USAGE_LOGGED | userId={} | endpoint={} | success={}",
                            user.getId(), fullUri, isSuccess);
                } catch (Exception e) {
                    log.error("Failed to create API usage log", e);
                }
            }
        }
    }

    /**
     * Check if endpoint should be excluded from API usage logging (e.g., static
     * resources, health checks, etc.)
     */
    private boolean isExcludedEndpoint(String uri) {
        return uri.startsWith("/apis/v1/api-docs")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/actuator")
                || uri.startsWith("/favicon.ico")
                || uri.startsWith("/static")
                || uri.startsWith("/css")
                || uri.startsWith("/js")
                || uri.startsWith("/images");
    }
}
