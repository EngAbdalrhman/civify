package com.app.civify.security.filter;
import com.app.civify.util.JwtConstants;
import com.app.civify.security.authenticationToken.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtValidationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;

    public JwtValidationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractJwtFromRequest(request);
        if (token != null) {

            try {
                JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(token);
                Authentication authResult = authenticationManager.authenticate(authenticationToken);
                if(authResult.isAuthenticated()) {
                    SecurityContextHolder.getContext().setAuthentication(authResult);
                }
            } catch (AuthenticationException e) {
                log.error("Token is invalid/expired. Proceed as anonymous, allow other filters (like refresh) to proceed");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }
        
        // Fallback: Check for auth_token cookie (required for SSE EventSource)
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (JwtConstants.ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}

