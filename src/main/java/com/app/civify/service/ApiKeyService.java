package com.app.civify.service;

import com.app.civify.entity.security.ApiKey;
import com.app.civify.entity.User;
import com.app.civify.entity.logs.ApiKeyUsageLog;
import com.app.civify.repository.security.ApiKeyRepository;
import com.app.civify.repository.logs.ApiKeyUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    private final ApiKeyUsageLogRepository apiKeyUsageLogRepository;

    private static final String KEY_PREFIX = "cv-fy-";
    private static final int KEY_LENGTH = 32;

    @Transactional
    public String createApiKey(User user, String name) {
        String rawKey = generateSecureKey();
        String keyHash = hashKey(rawKey);
        
        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPreview(maskKey(rawKey));
        apiKey.setName(name);
        apiKey.setIsActive(true);
        
        apiKeyRepository.save(apiKey);
        
        return rawKey;
    }

    public List<ApiKey> getUserApiKeys(User user) {
        return apiKeyRepository.findByUserId(user.getId());
    }

    @Transactional
    public void revokeApiKey(Long id, User user) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API Key not found"));
        
        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to API Key");
        }
        
        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
    }

    @Transactional(readOnly = true)
    public ApiKey validateKey(String rawKey) {
        String keyHash = hashKey(rawKey);
        ApiKey apiKey = apiKeyRepository.findByKeyHash(keyHash)
                .filter(ApiKey::getIsActive)
                .orElse(null);
        
        if (apiKey != null) {
            // Force initialization of User entity and subscriptions to avoid LazyInitializationException in filter
            apiKey.getUser().getId(); 
            apiKey.getUser().getEmail();
            // Initialize subscriptions for plan checks since they are accessed in the filter
            if (apiKey.getUser().getSubscriptions() != null) {
                apiKey.getUser().getSubscriptions().size();
            }
        }
        return apiKey;
    }

    @Transactional
    public void logUsage(User user, ApiKey apiKey, String endpoint, String method, int statusCode, boolean success, String ipAddress, String errorMessage) {
        ApiKeyUsageLog log = ApiKeyUsageLog.builder()
                .user(user)
                .apiKey(apiKey)
                .endpoint(endpoint)
                .method(method)
                .statusCode(statusCode)
                .success(success)
                .ipAddress(ipAddress)
                .errorMessage(errorMessage)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        apiKeyUsageLogRepository.save(log);

        if (apiKey != null) {
            apiKey.setLastUsedAt(java.time.LocalDateTime.now());
            apiKeyRepository.save(apiKey);
        }
    }

    private String generateSecureKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String randomString = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        // Ensure we only take what we need and it's clean
        return KEY_PREFIX + randomString.substring(0, KEY_LENGTH);
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing API key", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String maskKey(String key) {
        if (key.length() <= 12) return key;
        return key.substring(0, 8) + "..." + key.substring(key.length() - 4);
    }

    private static final String EXTENSION_KEY_NAME = "Chrome Extension";

    /**
     * Get or create an API key for Chrome Extension login.
     * If an active key named "Chrome Extension" exists, revokes it and creates a new one
     * (since we can't retrieve the raw key from the hash).
     * Returns the raw key for the extension to store.
     */
    @Transactional
    public String getOrCreateExtensionKey(User user) {
        // Revoke any existing extension key
        apiKeyRepository.findByUserIdAndNameAndIsActiveTrue(user.getId(), EXTENSION_KEY_NAME)
                .ifPresent(existingKey -> {
                    existingKey.setIsActive(false);
                    apiKeyRepository.save(existingKey);
                });

        // Create a fresh key
        return createApiKey(user, EXTENSION_KEY_NAME);
    }
}
