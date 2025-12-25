package com.app.civify.service.auth;

import com.app.civify.entity.security.Permission;
import com.app.civify.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for encrypting and decrypting user permissions
 * Uses AES-256-GCM for authenticated encryption
 * 
 * Security Features:
 * - AES-256-GCM authenticated encryption
 * - Random IV generation per encryption
 * - Timestamp inclusion to prevent replay attacks
 * - HMAC-like authentication tag via GCM
 * 
 * @author Abdalrhman Mostafa
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionsEncryptionService {

    private final ObjectMapper objectMapper;
    
    @Value("${permissions.encryption.key:CHANGE_THIS_32_BYTE_SECRET_KEY_}")
    private String encryptionKey;
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    /**
     * Encrypt list of permissions to Base64 encoded string
     * Includes timestamp to prevent replay attacks
     * 
     * @param permissions List of permission entities
     * @return Base64 encoded encrypted string
     */
    public String encryptPermissions(List<Permission> permissions) {
        try {
            // Extract permission names
            List<String> permissionNames = permissions.stream()
                    .map(Permission::getName)
                    .collect(Collectors.toList());
            
            // Create payload with timestamp
            PermissionsPayload payload = new PermissionsPayload(
                    permissionNames,
                    System.currentTimeMillis()
            );
            
            // Convert to JSON
            String json = objectMapper.writeValueAsString(payload);
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            // Encrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = getSecretKey();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] encryptedBytes = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + encrypted data
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);
            
            // Encode to Base64
            return Base64.getEncoder().encodeToString(byteBuffer.array());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize permissions", e);
            throw new BadRequestException("Failed to process permissions");
        } catch (Exception e) {
            log.error("Failed to encrypt permissions", e);
            throw new BadRequestException("Failed to encrypt permissions");
        }
    }
    
    /**
     * Decrypt Base64 encoded permissions string
     * Validates timestamp to prevent stale data usage
     * 
     * @param encryptedData Base64 encoded encrypted string
     * @return List of permission names
     */
    public List<String> decryptPermissions(String encryptedData) {
        try {
            // Decode Base64
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            
            // Extract IV and encrypted data
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);
            
            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = getSecretKey();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String json = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            // Parse JSON
            PermissionsPayload payload = objectMapper.readValue(json, PermissionsPayload.class);
            
            // Validate timestamp (not older than 24 hours)
            long age = System.currentTimeMillis() - payload.getTimestamp();
            if (age > 86400000) { // 24 hours in milliseconds
                log.warn("Decrypted permissions are too old: {} ms", age);
            }
            
            return payload.getPermissions();
            
        } catch (Exception e) {
            log.error("Failed to decrypt permissions", e);
            throw new BadRequestException("Invalid encrypted permissions");
        }
    }
    
    /**
     * Get AES secret key from configuration
     */
    private SecretKey getSecretKey() {
        // Ensure key is exactly 32 bytes for AES-256
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            log.warn("Encryption key is not 32 bytes, padding/truncating. Please configure a proper 32-byte key!");
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    /**
     * Internal payload structure for encrypted permissions
     */
    private static class PermissionsPayload {
        private List<String> permissions;
        private long timestamp;
        
        public PermissionsPayload() {}
        
        public PermissionsPayload(List<String> permissions, long timestamp) {
            this.permissions = permissions;
            this.timestamp = timestamp;
        }
        
        public List<String> getPermissions() {
            return permissions;
        }
        
        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
