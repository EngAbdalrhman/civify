package com.app.civify.security;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.security.model.Role;
import com.app.civify.service.auth.TokenService;
import com.app.civify.service.subscription.SubscriptionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * - Returns tokens immediately
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

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

        // Generate tokens
        TokenService.AuthTokenPair tokens = tokenService.createAuthTokens(user);

        // Redirect to frontend with tokens
        String targetUrl = "http://localhost:9002/auth/callback" +
                "?accessToken=" + tokens.getAccessToken() +
                "&refreshToken=" + tokens.getRefreshToken();

        response.sendRedirect(targetUrl);

        log.info("Redirecting to frontend: {}", targetUrl);
    }
}
