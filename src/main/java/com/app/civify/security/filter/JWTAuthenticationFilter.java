package com.app.civify.security.filter;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.exception.ErrorCode;
import com.app.civify.security.UserPrincipal;
import com.app.civify.util.CommonResponse;
import com.app.civify.util.JwtConstants;
import com.app.civify.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
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
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private final Boolean secureToken;
    private final Boolean emailVerificationEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!request.getServletPath().equals(JwtConstants.GENERATE_TOKEN_ENDPOINT)) {
            filterChain.doFilter(request, response);
            return;
        }

        logger.info("Logging...");
        ObjectMapper objectMapper = new ObjectMapper();
        var loginRequest = objectMapper.readValue(request.getInputStream(), AuthRequest.class);

        // Use whichever credential is provided (username or email)
        // Determine which credential to use for authentication
        UsernamePasswordAuthenticationToken authToken = getUsernamePasswordAuthenticationToken(loginRequest);

        try {
            Authentication authResult = authenticationManager.authenticate(authToken);

            if (authResult.isAuthenticated()) {
                // Manually check email verification if enabled
                if (emailVerificationEnabled) {
                   UserPrincipal principal = (UserPrincipal) authResult.getPrincipal();
                   if (!principal.getUser().getIsEmailVerified()) {
                        // Return proper JSON error response with error code
                        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            ErrorCode.AUTH_EMAIL_NOT_VERIFIED,
                            "Please verify your email before logging in. Check your inbox for verification link.");
                        return;
                   }
                }

                String token = jwtUtil.generateToken(authResult.getName(), JwtConstants.ACCESS_TOKEN_EXPIRATION_MINUTES);
                response.setHeader("Authorization", "Bearer " + token);

                String refreshToken = jwtUtil.generateToken(authResult.getName(), JwtConstants.REFRESH_TOKEN_EXPIRATION_MINUTES);

                // Set Refresh Token in HttpOnly Cookie
                Cookie refreshCookie = new Cookie(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken);
                refreshCookie.setHttpOnly(true); // prevent javascript from accessing it
                refreshCookie.setSecure(secureToken); // Set to false for development (HTTP), true for production (HTTPS)
                refreshCookie.setPath(JwtConstants.REFRESH_TOKEN_COOKIE_PATH);
                refreshCookie.setMaxAge(JwtConstants.REFRESH_TOKEN_COOKIE_MAX_AGE);
                response.addCookie(refreshCookie);
            }
        } catch (AuthenticationServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                ErrorCode.AUTH_INVALID_CREDENTIALS, e.getMessage());
        } catch (Exception e) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                ErrorCode.AUTH_INVALID_CREDENTIALS, "Authentication failed: " + e.getMessage());
        }
    }

    private static @NotNull UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(AuthRequest loginRequest) {
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
        return authToken;
    }

    /**
     * Write a structured JSON error response
     */
    private void writeErrorResponse(HttpServletResponse response, int status, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        CommonResponse<?> errorResponse = CommonResponse.error(errorCode, message);
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Register JavaTimeModule for proper date serialization
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
