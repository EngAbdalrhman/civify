package com.app.civify.security;

import com.app.civify.config.properties.AppProperties;
import com.app.civify.entity.User;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.repository.UserRepository;

import com.app.civify.service.auth.TokenService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.JwtConstants;
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
import java.util.UUID;

/**
 * OAuth2 success handler for Google login
 * - Auto-verifies email (Google already verified it)
 * - Creates user if doesn't exist
 * - Creates FREE subscription
 * - Returns tokens using TokenService for consistency
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final SubscriptionService subscriptionService;
    private final TokenService tokenService;
    private final AppProperties appProperties;

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
            newUser.setPassword(UUID.randomUUID().toString()); // No password for OAuth users
            newUser.setIsEmailVerified(true); // Auto-verified by Google
            newUser.setOauthProvider("google");
            newUser.setOauthSubject(googleId);
            newUser.setTwoFactorEnabled(false);

            // Assign default group
            groupRepository.findByIsDefaultTrue()
                    .ifPresent(newUser::setGroup);

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

        // Generate tokens using TokenService for proper lifecycle management
        // This ensures consistency with standard login and refresh token rotation
        TokenService.AuthTokenPair tokens = tokenService.createAuthTokens(user);

        // Set Refresh Token in HttpOnly Cookie
        Cookie refreshCookie = new Cookie(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, tokens.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Set to true in production with HTTPS
        refreshCookie.setPath(JwtConstants.REFRESH_TOKEN_COOKIE_PATH);
        refreshCookie.setMaxAge(JwtConstants.REFRESH_TOKEN_COOKIE_MAX_AGE);
        response.addCookie(refreshCookie);

        // Redirect to frontend with access token only (refresh token is in cookie)
        String targetUrl = appProperties.getFrontend().getBaseUrl() + "/auth/callback" +
                "?accessToken=" + tokens.getAccessToken();

        response.sendRedirect(targetUrl);

        log.info("OAuth2 tokens generated and redirecting to frontend");
    }
}
