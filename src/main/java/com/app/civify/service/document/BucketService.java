package com.app.civify.service.document;

import com.app.civify.entity.document.StoredFile;
import com.app.civify.exception.BadRequestException;
import com.app.civify.repository.document.StoredFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Optional;

@Service
@Slf4j
public class BucketService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StoredFileRepository storedFileRepository;

    public BucketService(
            @Autowired(required = false) S3Client s3Client,
            @Autowired(required = false) S3Presigner s3Presigner,
            StoredFileRepository storedFileRepository) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.storedFileRepository = storedFileRepository;
    }

    @Value("${civify.r2.enabled:true}")
    private boolean r2Enabled;

    @Value("${civify.r2.bucket:civify}")
    private String r2Bucket;

    @Value("${civify.r2.watermark-bucket:watermarks}")
    private String watermarkBucket;

    @Value("${civify.r2.dedup-enabled:true}")
    private boolean dedupEnabled;

    @Value("${civify.r2.public-base-url:}")
    private String publicBaseUrl;

    @Value("${civify.r2.watermark-public-base-url:}")
    private String watermarkPublicBaseUrl;

    @Value("${civify.r2.presign-ttl-minutes:15}")
    private int presignTtlMinutes;

    /** Object keys in {@code civify.r2.watermark-bucket} (default bucket name {@code watermarks}). */
    public static final String PLATFORM_WATERMARK_KEY = "platform/logo.png";
    private static final long MAX_WATERMARK_BYTES = 512_000;

    public static String userWatermarkKey(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId + "/logo.png";
    }

    /**
     * Normalize DB/R2 keys: legacy rows may use {@code watermarks/1/logo.png} while the
     * watermark bucket stores {@code 1/logo.png}.
     */
    public static String resolveWatermarkStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return storageKey;
        }
        if (storageKey.startsWith("watermarks/")) {
            return storageKey.substring("watermarks/".length());
        }
        return storageKey;
    }

    /**
     * Store CV in Cloudflare R2 bucket.
     * Path: cvs/YYYY-MM-DD/timestamp_filename
     */
    public void storeCv(MultipartFile file, Long userId) {
        if (!r2Enabled) {
            log.info("R2 storage is disabled, skipping CV upload.");
            return;
        }

        log.info("starting storing on s3 bucket");

        try {
            byte[] fileBytes = file.getBytes();
            String fileHash = computeSha256(fileBytes);

            if (dedupEnabled && fileHash != null) {
                Optional<StoredFile> existingFile = storedFileRepository.findFirstByFileHash(fileHash);
                if (existingFile.isPresent()) {
                    log.info("Duplicate file detected (hash: {}). Skipping S3 upload.", fileHash);
                    return;
                }
            }

            String dateFolder = LocalDate.now().toString(); // YYYY-MM-DD
            String originalFilename = file.getOriginalFilename();
            // Sanitize filename
            String safeFilename = originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "unknown.pdf";
            String uniqueFilename = System.currentTimeMillis() + "_" + safeFilename;
            String key = dateFolder + "/" + uniqueFilename;

            RequestBody requestBody = RequestBody.fromBytes(fileBytes);

            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(r2Bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build(), requestBody);

            log.info("Stored CV in R2 at: {}/{}", r2Bucket, key);

            saveBucketMetaData(file, userId, key, originalFilename, fileHash);

            log.info("stored metadata");

        } catch (Exception e) {
            log.error("Failed to store CV in R2: {}", e.getMessage(), e);
            // We do not throw exception here to avoid failing the whole parsing process if storage fails
        }
    }

    public String storePlatformWatermark(MultipartFile file) {
        return storeWatermarkAtKey(PLATFORM_WATERMARK_KEY, file);
    }

    public String storeUserWatermark(Long userId, MultipartFile file) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return storeWatermarkAtKey(userWatermarkKey(userId), file);
    }

    /**
     * PNG watermark rules (max 500KB, valid PNG header). Call before any DB update on upload.
     */
    public void validateWatermarkUpload(MultipartFile file) {
        validateWatermarkPng(file);
    }

    public void deleteObject(String key) {
        deleteFromBucket(r2Bucket, key);
    }

    public void deleteWatermarkObject(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        String normalized = resolveWatermarkStorageKey(key);
        deleteFromBucket(watermarkBucket, normalized);
        if (!normalized.equals(key)) {
            deleteFromBucket(watermarkBucket, key);
        }
    }

    private void deleteFromBucket(String bucket, String key) {
        if (!r2Enabled || s3Client == null || key == null || key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(b -> b.bucket(bucket).key(key));
            log.info("Deleted R2 object: {}/{}", bucket, key);
        } catch (Exception e) {
            log.error("Failed to delete R2 object {}/{}: {}", bucket, key, e.getMessage());
        }
    }

    /**
     * Public CDN/custom-domain URL for watermark bucket (optional).
     */
    public String buildPublicLogoUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        String baseUrl = watermarkPublicBaseUrl != null && !watermarkPublicBaseUrl.isBlank()
                ? watermarkPublicBaseUrl
                : publicBaseUrl;
        if (baseUrl != null && !baseUrl.isBlank()) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/" + resolveWatermarkStorageKey(storageKey);
        }
        return null;
    }

    /**
     * Presigned GET for watermark objects in the watermark bucket.
     */
    public String generatePresignedGetUrl(String storageKey) {
        return generatePresignedGetUrl(watermarkBucket, resolveWatermarkStorageKey(storageKey));
    }

    private String generatePresignedGetUrl(String bucket, String storageKey) {
        if (!r2Enabled || s3Presigner == null || storageKey == null || storageKey.isBlank()) {
            return null;
        }
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(Math.max(1, presignTtlMinutes)))
                    .getObjectRequest(getObjectRequest)
                    .build();
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Failed to presign R2 URL for {}/{}: {}", bucket, storageKey, e.getMessage());
            return null;
        }
    }

    /**
     * Resolve logo URL for PDF rendering: public CDN if configured, otherwise presigned R2 URL.
     */
    public String resolveLogoUrlForPdf(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        String normalized = resolveWatermarkStorageKey(storageKey);
        String url = resolveLogoUrlForKey(normalized);
        if (url == null && !normalized.equals(storageKey)) {
            url = resolveLogoUrlForKey(storageKey);
        }
        return url;
    }

    private String resolveLogoUrlForKey(String key) {
        String publicUrl = buildPublicLogoUrl(key);
        if (publicUrl != null) {
            return publicUrl;
        }
        return generatePresignedGetUrl(key);
    }

    private String storeWatermarkAtKey(String key, MultipartFile file) {
        if (!r2Enabled || s3Client == null) {
            log.error("R2 is not configured (enabled={}, client={}); cannot store watermark at {}",
                    r2Enabled, s3Client != null, key);
            throw new BadRequestException("storage.r2.unavailable");
        }
        validateWatermarkPng(file);
        try {
            byte[] fileBytes = file.getBytes();
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(watermarkBucket)
                    .key(key)
                    .contentType("image/png")
                    .contentLength((long) fileBytes.length)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
            log.info("Stored watermark in R2 at: {}/{}", watermarkBucket, key);
            return key;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to store watermark in R2 at {}/{}: {}", watermarkBucket, key, e.getMessage(), e);
            throw new BadRequestException("storage.r2.upload.failed");
        }
    }

    private void validateWatermarkPng(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Watermark file is required");
        }
        if (file.getSize() > MAX_WATERMARK_BYTES) {
            throw new IllegalArgumentException("Watermark file must be 500KB or smaller");
        }
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!"image/png".equalsIgnoreCase(contentType) && !filename.endsWith(".png")) {
            throw new IllegalArgumentException("Watermark must be a PNG image");
        }
        try (var in = file.getInputStream()) {
            byte[] header = in.readNBytes(8);
            if (header.length < 8
                    || header[0] != (byte) 0x89
                    || header[1] != 0x50
                    || header[2] != 0x4E
                    || header[3] != 0x47) {
                throw new IllegalArgumentException("Invalid PNG file");
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read watermark file", e);
        }
    }

    /**
     * Fetch file bytes from R2 using storage key.
     */
    public byte[] fetchFile(String key) {
        if (!r2Enabled) {
            return null;
        }
        try {
            return s3Client.getObject(software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(r2Bucket)
                    .key(key)
                    .build()).readAllBytes();
        } catch (Exception e) {
            log.error("Failed to fetch file from R2 key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * helper methods
     */

    private void saveBucketMetaData(MultipartFile file, Long userId, String key, String originalFilename, String fileHash) {
        // Save metadata to DB
        StoredFile storedFile = new StoredFile();
        storedFile.setUserId(userId != null ? userId : 0L); // 0 for guest/unknown
        storedFile.setFileName(originalFilename);
        storedFile.setFileType(file.getContentType());
        storedFile.setFileSize(file.getSize());
        storedFile.setStorageKey(key);
        storedFile.setBucketName(r2Bucket);
        storedFile.setFileHash(fileHash);
        try {
            storedFileRepository.save(storedFile);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Race condition: another concurrent request already stored this hash
            log.warn("Duplicate hash constraint hit for hash {}. File already stored by a concurrent request.", fileHash);
        }
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data);
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to compute SHA-256 hash", e);
            return null;
        }
    }
}
