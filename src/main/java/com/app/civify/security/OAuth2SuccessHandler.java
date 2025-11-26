package com.app.civify.security;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.security.model.Role;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.JwtConstants;
import com.app.civify.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 success handler for Google login
 * - Auto-verifies email (Google already verified it)
 * - Creates user if doesn't exist
 * - Creates FREE subscription
 * - Returns tokens using same method as standard login
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user info from Google
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");

        log.info("OAuth2 login successful for email: {}", email);

        // Find or create user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Creating new user from OAuth2: {}", email);

            // Create new user
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email); // Use email as username
            newUser.setPassword(""); // No password for OAuth users
            newUser.setRole(Role.ROLE_USER);
            newUser.setIsEmailVerified(true); // Auto-verified by Google
            newUser.setOauthProvider("google");
            newUser.setOauthSubject(googleId);
            newUser.setTwoFactorEnabled(false);

            if (name != null) {
                String[] nameParts = name.split(" ", 2);
                newUser.setFirstName(nameParts[0]);
                if (nameParts.length > 1) {
                    newUser.setLastName(nameParts[1]);
                }
            }

            User savedUser = userRepository.save(newUser);

            // Create default FREE subscription
            subscriptionService.createDefaultFreeSubscription(savedUser.getId());

            log.info("New OAuth2 user created and FREE subscription assigned: {}", email);

            return savedUser;
        });

        // Update last login
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens using SAME method as standard login
        String accessToken = jwtUtil.generateToken(user.getUsername(), JwtConstants.ACCESS_TOKEN_EXPIRATION_MINUTES);
        String refreshToken = jwtUtil.generateToken(user.getUsername(), JwtConstants.REFRESH_TOKEN_EXPIRATION_MINUTES);

        // Set Refresh Token in HttpOnly Cookie (same as standard login)
        Cookie refreshCookie = new Cookie(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Set to true in production with HTTPS
        refreshCookie.setPath(JwtConstants.REFRESH_TOKEN_COOKIE_PATH);
        refreshCookie.setMaxAge(JwtConstants.REFRESH_TOKEN_COOKIE_MAX_AGE);
        response.addCookie(refreshCookie);

        // Redirect to frontend with access token only (refresh token is in cookie)
        String targetUrl = frontendUrl + "/auth/callback" +
                "?accessToken=" + accessToken;

        response.sendRedirect(targetUrl);

        log.info("OAuth2 tokens generated and redirecting to frontend");
    }
}
