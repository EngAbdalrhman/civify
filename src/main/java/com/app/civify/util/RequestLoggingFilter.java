package com.app.civify.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Metadata only: never log headers, query strings, bodies, filenames or credentials. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String OPERATION_FAILED = RequestLoggingFilter.class.getName() + ".failed";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        Map<String, String> previous = MDC.getCopyOfContextMap();
        String suppliedId = request.getHeader("X-Request-ID");
        String requestId = suppliedId != null && suppliedId.matches("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")
                ? suppliedId : UUID.randomUUID().toString();
        String tool = request.getHeader("X-Civify-MCP-Tool");
        // Caller labels are diagnostics, never authentication or authorization evidence.
        if (tool == null || !tool.matches("civify_[a-z_]{1,64}")) tool = "none";
        String operation = operation(request.getRequestURI());
        String method = request.getMethod().replaceAll("[^A-Z]", "");
        response.setHeader("X-Request-ID", requestId);
        MDC.put("requestId", requestId);
        MDC.put("mcpTool", tool);
        MDC.put("operation", operation);
        boolean threw = false;
        log.info("REQUEST_START | requestId={} | method={} | operation={} | mcpTool={}", requestId, method, operation, tool);
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error failure) {
            threw = true;
            throw failure;
        } finally {
            int status = threw && response.getStatus() < 400 ? 500 : response.getStatus();
            boolean success = !threw && status < 400 && !Boolean.TRUE.equals(request.getAttribute(OPERATION_FAILED));
            log.info("REQUEST_END | requestId={} | method={} | operation={} | mcpTool={} | status={} | durationMs={} | success={}",
                    requestId, method, operation, tool, status, (System.nanoTime() - started) / 1_000_000, success);
            if (previous == null) MDC.clear(); else MDC.setContextMap(previous);
        }
    }

    private static String operation(String path) {
        // Only fixed route names enter logs. Resource IDs and arbitrary paths stay private.
        int external = path.indexOf("/external/");
        if (external < 0) return "http";
        String route = path.substring(external);
        return switch (route) {
            case "/external/cvs/parse" -> "cv_parse";
            case "/external/cvs/tailor" -> "cv_tailor";
            case "/external/cvs/mask" -> "cv_mask";
            case "/external/cvs/score" -> "cv_score";
            case "/external/cvs/scrape-jd" -> "job_scrape";
            case "/external/cvs/user/profile" -> "account_profile";
            case "/external/cvs/watermark-config" -> "pdf_policy";
            default -> route.startsWith("/external/job-applications") ? "job_applications"
                    : route.startsWith("/external/pay-per-cv") ? "pay_per_cv" : "external_other";
        };
    }
}
