package com.app.civify.service.i18n;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service for handling internationalized messages
 * Provides methods to retrieve localized messages based on the current locale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Get a localized message for the given key using the current locale
     *
     * @param key Message key from properties file
     * @return Localized message
     */
    public String getMessage(String key) {
        return getMessage(key, (Object[]) null);
    }

    /**
     * Get a localized message for the given key with parameters
     *
     * @param key    Message key from properties file
     * @param params Parameters to substitute in the message
     * @return Localized message with parameters substituted
     */
    public String getMessage(String key, Object[] params) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, params, locale);
        } catch (Exception e) {
            log.warn("Message not found for key: {} in locale: {}", key, LocaleContextHolder.getLocale());
            return key; // Return key if message not found
        }
    }

    /**
     * Get a localized message for a specific locale
     *
     * @param key    Message key from properties file
     * @param locale Specific locale to use
     * @return Localized message
     */
    public String getMessage(String key, Locale locale) {
        return getMessage(key, null, locale);
    }

    /**
     * Get a localized message for a specific locale with parameters
     *
     * @param key    Message key from properties file
     * @param params Parameters to substitute in the message
     * @param locale Specific locale to use
     * @return Localized message with parameters substituted
     */
    public String getMessage(String key, Object[] params, Locale locale) {
        try {
            return messageSource.getMessage(key, params, locale);
        } catch (Exception e) {
            log.warn("Message not found for key: {} in locale: {}", key, locale);
            return key; // Return key if message not found
        }
    }

    /**
     * Get the current locale from the context
     *
     * @return Current locale
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * Check if the current locale is RTL (Right-to-Left)
     *
     * @return true if current locale is RTL (Arabic), false otherwise
     */
    public boolean isRtl() {
        Locale locale = getCurrentLocale();
        return "ar".equals(locale.getLanguage());
    }

    /**
     * Get the language code of the current locale
     *
     * @return Language code (e.g., "en", "ar")
     */
    public String getCurrentLanguage() {
        return getCurrentLocale().getLanguage();
    }
}

