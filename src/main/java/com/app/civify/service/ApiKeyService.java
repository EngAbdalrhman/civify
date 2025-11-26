package com.app.civify.service;

import com.app.civify.entity.ApiKey;
import com.app.civify.entity.User;
import com.app.civify.repository.ApiKeyRepository;
import com.app.civify.repository.UserRepository;
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
    private final UserRepository userRepository;

    private static final String KEY_PREFIX = "cv-fy-";
    private static final int KEY_LENGTH = 32;

    @Transactional
    public String createApiKey(User user) {
        String rawKey = generateSecureKey();
        String keyHash = hashKey(rawKey);
        
        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPreview(maskKey(rawKey));
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
        
        apiKeyRepository.delete(apiKey);
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
}
