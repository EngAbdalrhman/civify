package com.app.civify.service.auth;

import com.app.civify.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Service for validating password strength and checking against known data breaches.
 * Uses Have I Been Pwned (HIBP) API with k-anonymity for privacy-preserving checks.
 */
@Service
@Slf4j
public class PasswordValidationService {

    private static final String HIBP_API_URL = "https://api.pwnedpasswords.com/range/";
    
    // Password complexity requirements:
    // - At least 8 characters
    // - At least one uppercase letter
    // - At least one lowercase letter  
    // - At least one digit
    // - At least one special character
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&^#()\\-_=+\\[\\]{}|;:',.<>/~`])[A-Za-z\\d@$!%*?&^#()\\-_=+\\[\\]{}|;:',.<>/~`]{8,}$"
    );
    
    private final HttpClient httpClient;

    public PasswordValidationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Validates password against complexity requirements and checks if it has been pwned.
     * 
     * @param password The password to validate
     * @throws BadRequestException if password doesn't meet requirements or has been breached
     */
    public void validatePassword(String password) {
        validateComplexity(password);
        checkPwned(password);
    }

    /**
     * Validates that password meets complexity requirements.
     */
    public void validateComplexity(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("auth.password.weak");
        }
    }

    /**
     * Checks if password has been found in known data breaches using HIBP API.
     * Uses k-anonymity: only the first 5 characters of the SHA-1 hash are sent to the API.
     */
    public void checkPwned(String password) {
        try {
            String sha1Hash = sha1(password).toUpperCase();
            String prefix = sha1Hash.substring(0, 5);
            String suffix = sha1Hash.substring(5);

            String response = queryHibpApi(prefix);
            if (response != null && isPwnedInResponse(response, suffix)) {
                log.warn("User attempted to register with a pwned password");
                throw new BadRequestException("auth.password.pwned");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            // Log but don't block registration if HIBP API is unavailable
            log.warn("Failed to check HIBP API, allowing registration: {}", e.getMessage());
        }
    }

    /**
     * Query HIBP API with the password hash prefix.
     * Results are cached to reduce API calls for common password prefixes.
     */
    @Cacheable(value = "hibpCache", key = "#prefix", unless = "#result == null")
    public String queryHibpApi(String prefix) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HIBP_API_URL + prefix))
                    .header("User-Agent", "Civify-PasswordChecker")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            }
            return null;
        } catch (Exception e) {
            log.debug("HIBP API request failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if the password suffix appears in the HIBP response.
     * Response format: HASH_SUFFIX:COUNT (one per line)
     */
    private boolean isPwnedInResponse(String response, String suffix) {
        String[] lines = response.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith(suffix + ":")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compute SHA-1 hash of the password.
     */
    private String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
