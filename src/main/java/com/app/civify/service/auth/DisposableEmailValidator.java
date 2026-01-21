package com.app.civify.service.auth;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Service to validate if an email address belongs to a disposable/temporary email provider.
 * Blocks registration attempts using known throwaway email domains.
 */
@Service
@Slf4j
public class DisposableEmailValidator {

    private static final String BLOCKLIST_PATH = "blocklists/disposable-email-domains.txt";

    @Value("${civify.security.block-disposable-emails:true}")
    private boolean blockDisposableEmails;

    private Set<String> disposableDomains;

    @PostConstruct
    public void init() {
        disposableDomains = loadDisposableDomains();
        log.info("Loaded {} disposable email domains. Blocking enabled: {}", 
                disposableDomains.size(), blockDisposableEmails);
    }

    /**
     * Check if the given email belongs to a known disposable email provider.
     *
     * @param email The email address to validate
     * @return true if the email is from a disposable domain, false otherwise
     */
    public boolean isDisposable(String email) {
        if (!blockDisposableEmails || email == null || email.isBlank()) {
            return false;
        }
        
        String domain = extractDomain(email);
        if (domain == null) {
            return false;
        }
        
        return disposableDomains.contains(domain.toLowerCase());
    }

    /**
     * Extract the domain part from an email address.
     */
    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1).toLowerCase().trim();
    }

    /**
     * Load the list of disposable email domains from the classpath resource.
     */
    private Set<String> loadDisposableDomains() {
        Set<String> domains = new HashSet<>();
        
        try {
            ClassPathResource resource = new ClassPathResource(BLOCKLIST_PATH);
            if (!resource.exists()) {
                log.warn("Disposable email blocklist not found at: {}. Using empty list.", BLOCKLIST_PATH);
                return domains;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    // Skip empty lines and comments
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        domains.add(line);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to load disposable email domains: {}", e.getMessage());
        }
        
        return domains;
    }
}
