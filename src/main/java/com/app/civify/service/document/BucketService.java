package com.app.civify.service.document;

import com.app.civify.entity.document.StoredFile;
import com.app.civify.repository.document.StoredFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Optional;

@Service
@Slf4j
public class BucketService {

    private final S3Client s3Client;
    private final StoredFileRepository storedFileRepository;

    public BucketService(@Autowired(required = false) S3Client s3Client, StoredFileRepository storedFileRepository) {
        this.s3Client = s3Client;
        this.storedFileRepository = storedFileRepository;
    }

    @Value("${civify.r2.enabled:true}")
    private boolean r2Enabled;

    @Value("${civify.r2.bucket:civify}")
    private String r2Bucket;

    @Value("${civify.r2.dedup-enabled:true}")
    private boolean dedupEnabled;


    /**
     * Store CV in Cloudflare R2 bucket.
     * Path: cvs/YYYY-MM-DD/timestamp_filename
     */
    public String storeCv(MultipartFile file, Long userId) {

        try {
            byte[] fileBytes = file.getBytes();
            String fileHash = computeSha256(fileBytes);

            if (!r2Enabled) {
                log.info("R2 storage is disabled, skipping CV upload.");
                return fileHash;
            }

            log.info("starting storing on s3 bucket");

            if (dedupEnabled && fileHash != null) {
                Optional<StoredFile> existingFile = storedFileRepository.findFirstByFileHash(fileHash);
                if (existingFile.isPresent()) {
                    log.info("Duplicate file detected (hash: {}). Skipping S3 upload.", fileHash);
                    return fileHash;
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

            return fileHash;

        } catch (Exception e) {
            log.error("Failed to store CV in R2: {}", e.getMessage(), e);
        }
        return null;
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
