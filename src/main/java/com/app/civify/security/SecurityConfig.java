package com.app.civify.security;

import com.app.civify.security.authenticationProvider.JWTAuthenticationProvider;
import com.app.civify.security.filter.JWTAuthenticationFilter;
import com.app.civify.security.filter.JWTRefreshFilter;
import com.app.civify.security.filter.JwtValidationFilter;
import com.app.civify.filter.RateLimitFilter;
import com.app.civify.service.auth.TokenService;
import com.app.civify.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.app.civify.util.JwtConstants.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final TokenService tokenService;

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    @Value("${payment.fawaterak.base-url}")
    private String paymentGateway;

    @Value("${refresh.token.secure}")
    private Boolean secureToken;

    @Value("${email.verification.enabled:true}")
    private Boolean emailVerificationEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    public JWTAuthenticationProvider jwtAuthenticationProvider() {
        return new JWTAuthenticationProvider(jwtUtil, userDetailsService);
    }


    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Arrays.asList(
                daoAuthenticationProvider(),
                jwtAuthenticationProvider()
        ));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {


        // Authentication filter responsible for login
        JWTAuthenticationFilter jwtAuthFilter = new JWTAuthenticationFilter(authenticationManager, jwtUtil, secureToken, emailVerificationEnabled);

        // Validation filter for checking JWT in every request
        JwtValidationFilter jwtValidationFilter = new JwtValidationFilter(authenticationManager);

        // refresh filter for checking JWT in every request
        JWTRefreshFilter jwtRefreshFilter = new JWTRefreshFilter(tokenService, userDetailsService, secureToken);


        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")))
                .authorizeHttpRequests(auth -> auth
                        // Authentication
                        .requestMatchers("/auth", "/auth/**").permitAll()
                        // Documentation endpoints
                        .requestMatchers("/civify-docs-ui", "/civify-docs-api", "/swagger-ui/**").permitAll()
                        // Public API endpoints
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/v1/subscriptions/**", "/subscriptions/**").permitAll() // Allow public access to plans
                        .requestMatchers("/v1/payments/config", "/payments/config").permitAll() // Allow public access to payment config
                        .requestMatchers("/v1/payments/webhook/**", "/payments/webhook/**").permitAll() // Allow payment webhooks
                        // Newsletter (Public)
                        .requestMatchers("/v1/newsletter/**").permitAll()
                        // External API endpoints (handled by ApiKeyAuthenticationFilter)
                        .requestMatchers("/v1/external/**").permitAll()
                        // Admin endpoints
                        .requestMatchers("/v1/admin/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class) // Rate limit first
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // API Key filter first
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // generate token filter
                .addFilterAfter(jwtValidationFilter, JWTAuthenticationFilter.class) // validate token filter
                .addFilterAfter(jwtRefreshFilter, JwtValidationFilter.class) // refresh token filter
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            // Log the error for debugging
                            LoggerFactory.getLogger(SecurityConfig.class)
                                    .error("OAuth2 login failed: {}", exception.getMessage(), exception);

                            // Redirect to frontend callback with error parameter
                            String errorMessage = exception.getMessage() != null
                                    ? exception.getMessage()
                                    : "OAuth login failed";
                            String targetUrl = frontendUrl + "/auth/callback?error=" +
                                    URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
                            response.sendRedirect(targetUrl);
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .addLogoutHandler((request, response, authentication) -> {
                            // 1. Blacklist Access Token
                            String accessToken = null;
                            String authHeader = request.getHeader("Authorization");
                            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                accessToken = authHeader.substring(7);
                            }
                            if (accessToken != null) {
                                tokenService.blacklistAccessToken(accessToken);
                            }

                            // 2. Revoke Refresh Token
                            String refreshToken = null;
                            if (request.getCookies() != null) {
                                for (Cookie cookie : request.getCookies()) {
                                    if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                                        refreshToken = cookie.getValue();
                                        break;
                                    }
                                }
                            }
                            if (refreshToken != null) {
                                tokenService.revokeRefreshToken(refreshToken);
                            }
                        })
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // Clear cookies on logout
                            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
                            refreshTokenCookie.setPath(TOKEN_COOKIE_PATH);
                            refreshTokenCookie.setMaxAge(0); // Set max age to 0 to delete the cookie
                            refreshTokenCookie.setHttpOnly(true);
                            refreshTokenCookie.setSecure(secureToken);
                            response.addCookie(refreshTokenCookie);

                            Cookie accessTokenCookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, null);
                            accessTokenCookie.setPath(TOKEN_COOKIE_PATH);
                            accessTokenCookie.setMaxAge(0); // Set max age to 0 to delete the cookie
                            accessTokenCookie.setHttpOnly(false); // As per JWTAuthenticationFilter
                            accessTokenCookie.setSecure(secureToken);
                            response.addCookie(accessTokenCookie);

                            response.setStatus(HttpStatus.OK.value());
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use setAllowedOriginPatterns to support wildcards with allowCredentials=true
        configuration.setAllowedOriginPatterns(List.of(
                frontendUrl, 
                "http://localhost:3000", 
                paymentGateway, 
                "www.".concat(frontendUrl),
                // Zoho CRM domains for extension widgets
                "https://crm.zoho.com",
                "https://crm.zoho.eu",
                "https://crm.zoho.in",
                "https://crm.zoho.com.au",
                "https://crmsandbox.zoho.com",
                "https://*.zappsusercontent.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie", "Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
