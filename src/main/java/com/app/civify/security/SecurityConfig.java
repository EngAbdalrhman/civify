package com.app.civify.security;

import com.app.civify.security.authenticationProvider.JWTAuthenticationProvider;
import com.app.civify.security.filter.JWTAuthenticationFilter;
import com.app.civify.security.filter.JWTRefreshFilter;
import com.app.civify.security.filter.JwtValidationFilter;
import com.app.civify.filter.RateLimitFilter;
import com.app.civify.util.JwtUtil;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

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
        JWTRefreshFilter jwtRefreshFilter = new JWTRefreshFilter(jwtUtil, authenticationManager);


        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .csrf(csrf -> csrf.disable())
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
                        // Guest CV operations (SaaS trial)
                        .requestMatchers("/v1/cvs/upload", "/v1/cvs/tailor").permitAll()
                        // External API endpoints (handled by ApiKeyAuthenticationFilter)
                        .requestMatchers("/api/v1/external/**").permitAll()
                        // Admin endpoints
                        // Admin endpoints - Protected by Method Security / CivifyAuthorization
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
                            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                                .error("OAuth2 login failed: {}", exception.getMessage(), exception);
                            
                            // Redirect to frontend callback with error parameter
                            String errorMessage = exception.getMessage() != null 
                                ? exception.getMessage() 
                                : "OAuth login failed";
                            String targetUrl = frontendUrl + "/auth/callback?error=" + 
                                java.net.URLEncoder.encode(errorMessage, java.nio.charset.StandardCharsets.UTF_8);
                            response.sendRedirect(targetUrl);
                        })
                );
        ;


        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(java.util.List.of(frontendUrl,"http://localhost:3000", paymentGateway));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setExposedHeaders(java.util.List.of("Authorization", "Set-Cookie"));
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
