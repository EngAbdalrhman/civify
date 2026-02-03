package com.app.civify.security.filter;

import com.app.civify.entity.User;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.auth.TokenService;
import com.app.civify.util.JwtConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.app.civify.util.JwtConstants.GENERATE_REFRESH_TOKEN_ENDPOINT;

@Slf4j
public class JWTRefreshFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;
    private final Boolean secureToken;

    public JWTRefreshFilter(TokenService tokenService, UserDetailsService userDetailsService, Boolean secureToken) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
        this.secureToken = secureToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        if (!request.getServletPath().equals(GENERATE_REFRESH_TOKEN_ENDPOINT)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String refreshToken = extractJwtFromRequest(request);
            if (refreshToken == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String username = tokenService.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            log.info("start generating refresh token for {}", userDetails.getUsername());

            if (!(userDetails instanceof UserPrincipal)) {
                 log.error("UserDetails is type: {}", userDetails.getClass().getName());
                 throw new ServletException("UserDetails is not of type UserPrincipal");
            }
            
            User user = ((UserPrincipal) userDetails).getUser();

            TokenService.AuthTokenPair tokenPair = tokenService.refreshAccessToken(refreshToken, user);

            // Set new Access Token Cookie
            ResponseCookie accessCookie = ResponseCookie.from(JwtConstants.ACCESS_TOKEN_COOKIE_NAME, tokenPair.getAccessToken())
                    .httpOnly(false)
                    .secure(secureToken)
                    .path(JwtConstants.TOKEN_COOKIE_PATH)
                    .maxAge(JwtConstants.ACCESS_TOKEN_COOKIE_MAX_AGE)
                    .sameSite("None")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

            // Set new Refresh Token Cookie (Rotation!)
            ResponseCookie refreshCookie = ResponseCookie.from(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, tokenPair.getRefreshToken())
                    .httpOnly(true)
                    .secure(secureToken)
                    .path(JwtConstants.TOKEN_COOKIE_PATH)
                    .maxAge(JwtConstants.REFRESH_TOKEN_COOKIE_MAX_AGE)
                    .sameSite("None")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            response.setHeader("Authorization", "Bearer " + tokenPair.getAccessToken());

            // Write token to response body as JSON (required by frontend)
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format("{\"accessToken\": \"%s\"}", tokenPair.getAccessToken()));
            response.getWriter().flush();

        } catch (Exception e) {
            // Token invalid, expired, or blacklisted
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid refresh token\"}");
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if (JwtConstants.REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                refreshToken = cookie.getValue();
            }
        }
        return refreshToken;
    }
}
