package com.app.civify.util.security;

import com.app.civify.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for encoding and decoding entity IDs.
 * Uses HMAC-SHA256 for signature and Base62 for URL-safe encoding.
 * 
 * Format: {prefix}{base62EncodedPayload}
 * Payload: {id}{signature(first 4 bytes)}
 */
@Component
@Slf4j
public class IdEncodingUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SIGNATURE_BYTES = 4;

    @Value("${id.encoding.secret:CHANGE_THIS_ID_ENCODING_SECRET_KEY}")
    private String secretKey;

    /**
     * Encode a Long ID with entity type validation
     * 
     * @param id         The numeric ID to encode
     * @param entityType The type of entity (prevents ID reuse attacks)
     * @return Encoded string ID
     */
    public String encode(Long id, EntityType entityType) {
        if (id == null || id <= 0) {
            throw new BadRequestException("Invalid ID for encoding");
        }

        try {
            // Create payload: id bytes + signature
            byte[] idBytes = longToBytes(id);
            byte[] signature = createSignature(id, entityType);
            byte[] truncatedSig = new byte[SIGNATURE_BYTES];
            System.arraycopy(signature, 0, truncatedSig, 0, SIGNATURE_BYTES);

            // Combine id + truncated signature
            ByteBuffer buffer = ByteBuffer.allocate(idBytes.length + SIGNATURE_BYTES);
            buffer.put(idBytes);
            buffer.put(truncatedSig);

            // Encode to Base62
            String encoded = toBase62(buffer.array());

            // Prefix with entity type
            return entityType.getPrefix() + encoded;

        } catch (Exception e) {
            log.error("Failed to encode ID: {} for type: {}", id, entityType, e);
            throw new BadRequestException("Failed to encode ID");
        }
    }

    /**
     * Decode an encoded ID string back to Long
     * 
     * @param encodedId  The encoded string ID
     * @param entityType Expected entity type (for validation)
     * @return The original numeric ID
     */
    public Long decode(String encodedId, EntityType entityType) {
        if (encodedId == null || encodedId.length() < 2) {
            throw new BadRequestException("Invalid encoded ID");
        }

        try {
            // Validate prefix
            String prefix = encodedId.substring(0, 1);
            if (!prefix.equals(entityType.getPrefix())) {
                log.warn("Entity type mismatch. Expected: {}, Got prefix: {}", entityType.getPrefix(), prefix);
                throw new BadRequestException("Invalid ID format");
            }

            // Decode from Base62
            String base62Part = encodedId.substring(1);
            byte[] decoded = fromBase62(base62Part);

            if (decoded.length < Long.BYTES + SIGNATURE_BYTES) {
                throw new BadRequestException("Invalid encoded ID length");
            }

            // Extract ID and signature
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] idBytes = new byte[Long.BYTES];
            buffer.get(idBytes);
            byte[] providedSig = new byte[SIGNATURE_BYTES];
            buffer.get(providedSig);

            Long id = bytesToLong(idBytes);

            // Verify signature
            byte[] expectedSig = createSignature(id, entityType);
            byte[] truncatedExpected = new byte[SIGNATURE_BYTES];
            System.arraycopy(expectedSig, 0, truncatedExpected, 0, SIGNATURE_BYTES);

            if (!constantTimeEquals(providedSig, truncatedExpected)) {
                log.warn("Signature mismatch for encoded ID: {}", encodedId);
                throw new BadRequestException("Invalid ID");
            }

            return id;

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to decode ID: {}", encodedId, e);
            throw new BadRequestException("Invalid ID format");
        }
    }

    /**
     * Check if an encoded ID is valid without throwing exceptions
     */
    public boolean isValid(String encodedId, EntityType entityType) {
        try {
            decode(encodedId, entityType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] createSignature(Long id, EntityType entityType) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);

        // Include entity type in signature to prevent cross-entity attacks
        String payload = entityType.name() + ":" + id;
        return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    private long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer.getLong();
    }

    private String toBase62(byte[] bytes) {
        // Convert bytes to a positive big integer value
        java.math.BigInteger value = new java.math.BigInteger(1, bytes);
        StringBuilder result = new StringBuilder();

        java.math.BigInteger base = java.math.BigInteger.valueOf(62);
        while (value.compareTo(java.math.BigInteger.ZERO) > 0) {
            int remainder = value.mod(base).intValue();
            result.insert(0, BASE62_CHARS.charAt(remainder));
            value = value.divide(base);
        }

        // Ensure minimum length
        while (result.length() < 12) {
            result.insert(0, '0');
        }

        return result.toString();
    }

    private byte[] fromBase62(String encoded) {
        java.math.BigInteger value = java.math.BigInteger.ZERO;
        java.math.BigInteger base = java.math.BigInteger.valueOf(62);

        for (char c : encoded.toCharArray()) {
            int digit = BASE62_CHARS.indexOf(c);
            if (digit < 0) {
                throw new BadRequestException("Invalid character in encoded ID");
            }
            value = value.multiply(base).add(java.math.BigInteger.valueOf(digit));
        }

        // Convert back to bytes (12 bytes for Long + 4 bytes signature)
        byte[] bytes = value.toByteArray();

        // Handle sign byte and padding
        int expectedLength = Long.BYTES + SIGNATURE_BYTES;
        if (bytes.length == expectedLength + 1 && bytes[0] == 0) {
            // Remove leading zero (sign byte)
            byte[] trimmed = new byte[expectedLength];
            System.arraycopy(bytes, 1, trimmed, 0, expectedLength);
            return trimmed;
        } else if (bytes.length < expectedLength) {
            // Pad with leading zeros
            byte[] padded = new byte[expectedLength];
            System.arraycopy(bytes, 0, padded, expectedLength - bytes.length, bytes.length);
            return padded;
        }

        return bytes;
    }

    /**
     * Constant-time comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
