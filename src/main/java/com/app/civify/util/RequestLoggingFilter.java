package com.app.civify.util;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
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

        // Add to MDC for thread-local access
        MDC.put("requestId", requestId);
        MDC.put("method", method);
        MDC.put("uri", fullUri);

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
            
            // Clear MDC
            MDC.clear();
        }
    }
}
