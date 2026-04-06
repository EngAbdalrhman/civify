package com.app.civify.security;

import com.app.civify.entity.User;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.repository.UserRepository;

import com.app.civify.security.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.app.civify.service.auth.ExtensionOAuthCodeService;
import com.app.civify.service.auth.TokenService;
import com.app.civify.service.referral.ReferralService;
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
import java.nio.charset.StandardCharsets;
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
    private final ReferralService referralService;
    private final ExtensionOAuthCodeService extensionOAuthCodeService;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    @Value("${refresh.token.secure}")
    private Boolean secureToken;

    @Value("${app.chrome-extension.id:}")
    private String chromeExtensionId;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user info from OAuth2 provider
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String providerId = oauth2User.getAttribute("sub"); // 'sub' is standard in OIDC for Google & LinkedIn

        log.info("OAuth2 login successful for email: {}", email);

        // Extract provider from authentication token
        String provider = "google"; // Default fallback
        if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken authToken) {
            provider = authToken.getAuthorizedClientRegistrationId();
        }

        // Find or create user
        String finalProvider = provider;
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Creating new user from OAuth2 provider [{}]: {}", finalProvider, email);

            // Create new user
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email); // Use email as username
            newUser.setPassword(UUID.randomUUID().toString()); // No password for OAuth users
            newUser.setIsEmailVerified(true); // Auto-verified by provider
            newUser.setOauthProvider(finalProvider);
            newUser.setOauthSubject(providerId);
            newUser.setTwoFactorEnabled(false);
            newUser.setReferralCode(referralService.generateUniqueCode());

            // Assign default group
            groupRepository.findByIsDefaultTrue()
                    .ifPresent(newUser::setGroup);

            if (name != null) {
                String[] nameParts = name.split(" ", 2);
                newUser.setFirstName(nameParts[0]);
                if (nameParts.length > 1) {
                    newUser.setLastName(nameParts[1]);
                }
            } else {
                 newUser.setFirstName("User");
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
        refreshCookie.setSecure(secureToken);
        refreshCookie.setPath(JwtConstants.TOKEN_COOKIE_PATH);
        refreshCookie.setMaxAge(JwtConstants.REFRESH_TOKEN_COOKIE_MAX_AGE);
        response.addCookie(refreshCookie);

        String extensionRedirectUri = extractExtensionRedirectUri(request);
        String targetUrl;
        if (extensionRedirectUri != null) {
            String oneTimeCode = extensionOAuthCodeService.issueCode(tokens.getAccessToken());
            targetUrl = extensionRedirectUri + "?code=" + oneTimeCode;
            log.info("OAuth2 extension login completed with one-time code redirect");
        } else {
            // Redirect to frontend with access token only (refresh token is in cookie)
            targetUrl = frontendUrl + "/auth/callback" +
                    "?accessToken=" + tokens.getAccessToken();
        }

        response.sendRedirect(targetUrl);
        cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        log.info("OAuth2 tokens generated and redirect completed");
    }

    private String extractExtensionRedirectUri(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (!HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME.equals(cookie.getName())) {
                continue;
            }

            String value = cookie.getValue();
            if (value == null || value.isBlank()) {
                return null;
            }

            String decoded = java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
            if (!decoded.startsWith("chrome-extension://")) {
                return null;
            }

            if (chromeExtensionId == null || chromeExtensionId.isBlank()) {
                return null;
            }

            String expectedPrefix = "chrome-extension://" + chromeExtensionId + "/";
            return decoded.startsWith(expectedPrefix) ? decoded : null;
        }

        return null;
    }
}
