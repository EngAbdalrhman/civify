package com.app.civify.security.filter;

import com.app.civify.entity.User;
import com.app.civify.service.auth.TokenService;
import com.app.civify.util.JwtConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

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

            if (!(userDetails instanceof User user)) {
                 log.error("user casting issue");
                 throw new ServletException("UserDetails is not of type com.app.civify.entity.User");
            }

            TokenService.AuthTokenPair tokenPair = tokenService.refreshAccessToken(refreshToken, user);

            // Set new Access Token Cookie
            Cookie accessCookie = new Cookie(JwtConstants.ACCESS_TOKEN_COOKIE_NAME, tokenPair.getAccessToken());
            accessCookie.setHttpOnly(false); // Accessible to JS
            accessCookie.setSecure(secureToken);
            accessCookie.setPath(JwtConstants.TOKEN_COOKIE_PATH);
            accessCookie.setMaxAge(JwtConstants.ACCESS_TOKEN_COOKIE_MAX_AGE);
            response.addCookie(accessCookie);

            // Set new Refresh Token Cookie (Rotation!)
            Cookie refreshCookie = new Cookie(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, tokenPair.getRefreshToken());
            refreshCookie.setHttpOnly(true); // HttpOnly
            refreshCookie.setSecure(secureToken);
            refreshCookie.setPath(JwtConstants.TOKEN_COOKIE_PATH);
            refreshCookie.setMaxAge(JwtConstants.REFRESH_TOKEN_COOKIE_MAX_AGE);
            response.addCookie(refreshCookie);

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
