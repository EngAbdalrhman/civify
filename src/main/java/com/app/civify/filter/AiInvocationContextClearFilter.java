package com.app.civify.filter;

import com.app.civify.service.ai.context.AiInvocationContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Clears AI invocation ThreadLocal after each HTTP request so pooled threads
 * do not leak BYOK billing context into unrelated requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiInvocationContextClearFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            AiInvocationContextHolder.clear();
        }
    }
}
