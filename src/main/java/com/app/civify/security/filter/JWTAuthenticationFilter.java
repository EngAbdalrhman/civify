package com.app.civify.security.filter;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.util.JwtConstants;
import com.app.civify.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter Handles login via /generate-token endpoint Supports
 * both username and email authentication
 */
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!request.getServletPath().equals(JwtConstants.GENERATE_TOKEN_ENDPOINT)) {
            filterChain.doFilter(request, response);
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        var loginRequest = objectMapper.readValue(request.getInputStream(), AuthRequest.class);

        // Use whichever credential is provided (username or email)
        // Determine which credential to use for authentication
        String credential = null;
        if (loginRequest.getUsername() != null && !loginRequest.getUsername().trim().isEmpty()) {
            credential = loginRequest.getUsername();
        } else if (loginRequest.getEmail() != null && !loginRequest.getEmail().trim().isEmpty()) {
            credential = loginRequest.getEmail();
        }

        if (credential == null) {
            throw new AuthenticationServiceException("Either username or email must be provided");
        }

        // Create authentication token with the provided credential and password
        UsernamePasswordAuthenticationToken authToken
                = new UsernamePasswordAuthenticationToken(credential, loginRequest.getPassword());

        try {
            Authentication authResult = authenticationManager.authenticate(authToken);

            if (authResult.isAuthenticated()) {
                String token = jwtUtil.generateToken(authResult.getName(), JwtConstants.ACCESS_TOKEN_EXPIRATION_MINUTES);
                response.setHeader("Authorization", "Bearer " + token);

                String refreshToken = jwtUtil.generateToken(authResult.getName(), JwtConstants.REFRESH_TOKEN_EXPIRATION_MINUTES);

                // Set Refresh Token in HttpOnly Cookie
                Cookie refreshCookie = new Cookie(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken);
                refreshCookie.setHttpOnly(true); // prevent javascript from accessing it
                refreshCookie.setSecure(true); // sent only over HTTPS
                refreshCookie.setPath(JwtConstants.REFRESH_TOKEN_COOKIE_PATH);
                refreshCookie.setMaxAge(JwtConstants.REFRESH_TOKEN_COOKIE_MAX_AGE);
                response.addCookie(refreshCookie);
            }
        } catch (AuthenticationServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationServiceException("Authentication failed: " + e.getMessage(), e);
        }
    }
}
