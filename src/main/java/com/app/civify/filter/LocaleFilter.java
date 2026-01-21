package com.app.civify.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Filter to set the locale context from Accept-Language header early in the filter chain.
 * This ensures that i18n messages can be properly localized even in exception handlers.
 * 
 * This filter runs with highest priority to ensure the locale is available for all
 * subsequent filters and exception handlers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocaleFilter extends OncePerRequestFilter {

    private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(
            Locale.ENGLISH,
            new Locale("ar")
    );
    
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Locale locale = resolveLocale(request);
            LocaleContextHolder.setLocale(locale);
            filterChain.doFilter(request, response);
        } finally {
            // Clear the locale context after the request is processed
            LocaleContextHolder.resetLocaleContext();
        }
    }

    /**
     * Resolve locale from Accept-Language header.
     * Falls back to default locale if header is missing or unsupported.
     */
    private Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return DEFAULT_LOCALE;
        }
        
        // Parse Accept-Language header (e.g., "ar,en-US;q=0.9,en;q=0.8")
        String primaryLanguage = acceptLanguage.split(",")[0].split(";")[0].trim();
        
        // Extract language code (e.g., "ar" from "ar-SA" or "en" from "en-US")
        String languageCode = primaryLanguage.split("-")[0].toLowerCase();
        
        // Find matching supported locale
        for (Locale supported : SUPPORTED_LOCALES) {
            if (supported.getLanguage().equalsIgnoreCase(languageCode)) {
                return supported;
            }
        }
        
        return DEFAULT_LOCALE;
    }
}
