package com.app.civify.security.filter;

import com.app.civify.security.authenticationToken.JwtAuthenticationToken;
import com.app.civify.util.JwtConstants;
import com.app.civify.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.app.civify.util.JwtConstants.ACCESS_TOKEN_EXPIRATION_MINUTES;
import static com.app.civify.util.JwtConstants.GENERATE_REFRESH_TOKEN_ENDPOINT;

public class JWTRefreshFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final Boolean secureToken;

    public JWTRefreshFilter(JwtUtil jwtUtil, AuthenticationManager authenticationManager, Boolean secureToken) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
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
        String refreshToken = extractJwtFromRequest(request);
        if (refreshToken == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(refreshToken);
        Authentication authResult = authenticationManager.authenticate(authenticationToken);
        if(authResult.isAuthenticated()) {
            String newToken = jwtUtil.generateToken(authResult.getName(), ACCESS_TOKEN_EXPIRATION_MINUTES);
            response.setHeader("Authorization", "Bearer " + newToken);

            Cookie accessCookie = new Cookie(JwtConstants.ACCESS_TOKEN_COOKIE_NAME, newToken);
            accessCookie.setHttpOnly(false);
            accessCookie.setSecure(secureToken);
            accessCookie.setPath(JwtConstants.TOKEN_COOKIE_PATH);
            accessCookie.setMaxAge(JwtConstants.ACCESS_TOKEN_COOKIE_MAX_AGE);
            response.addCookie(accessCookie);

            // Write token to response body as JSON (required by frontend)
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format("{\"accessToken\": \"%s\"}", newToken));
            response.getWriter().flush();
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
