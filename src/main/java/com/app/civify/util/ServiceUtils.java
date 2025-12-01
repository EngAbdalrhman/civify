package com.app.civify.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details Utils for services
 * @since 1.0.0
 * @version 1.0.0
 * @date 2025-10-17
 *
 */
public final class ServiceUtils {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    @Value("${civify.hashing.salt:civify_salt}")
    private String salt;

    /**
     * Calculates the SHA-256 hash of a given input string.
     *
     * @param input The string to hash.
     * @return The SHA-256 hash as a hexadecimal string.
     */
    public String getSha256Hash(String input) {
        try {

            input += salt;
            // Get an instance of MessageDigest for SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Compute the hash of the input string.
            // StandardCharsets.UTF_8 is important to ensure consistency across different systems.
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert the byte array into a signum representation.
            BigInteger number = new BigInteger(1, hash);

            // Convert the BigInteger into a hexadecimal string.
            StringBuilder hexString = new StringBuilder(number.toString(16));

            // Pad with leading zeros to ensure the hash is 64 characters long (256 bits).
            while (hexString.length() < 64) {
                hexString.insert(0, '0');
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // This exception is thrown if the algorithm is not available in the environment.
            // For standard algorithms like SHA-256, this is highly unlikely.
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, hash));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }

}
