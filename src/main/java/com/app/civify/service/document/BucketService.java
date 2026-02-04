package com.app.civify.service.document;

import com.app.civify.entity.document.StoredFile;
import com.app.civify.repository.document.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class BucketService {

    private final S3Client s3Client;
    private final StoredFileRepository storedFileRepository;

    @Value("${civify.r2.enabled:true}")
    private boolean r2Enabled;

    @Value("${civify.r2.bucket:civify}")
    private String r2Bucket;


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
            String dateFolder = LocalDate.now().toString(); // YYYY-MM-DD
            String originalFilename = file.getOriginalFilename();
            // Sanitize filename
            String safeFilename = originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "unknown.pdf";
            String uniqueFilename = System.currentTimeMillis() + "_" + safeFilename;
            String key = dateFolder + "/" + uniqueFilename;

            RequestBody requestBody =
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize());

            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(r2Bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build(), requestBody);

            log.info("Stored CV in R2 at: {}/{}", r2Bucket, key);

            saveBucketMetaData(file, userId, key, originalFilename);

            log.info("stored metadata");

        } catch (Exception e) {
            log.error("Failed to store CV in R2: {}", e.getMessage(), e);
            // We do not throw exception here to avoid failing the whole parsing process if storage fails
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

    private void saveBucketMetaData(MultipartFile file, Long userId, String key, String originalFilename) {
        // Save metadata to DB
        StoredFile storedFile = new StoredFile();
        storedFile.setUserId(userId != null ? userId : 0L); // 0 for guest/unknown
        storedFile.setFileName(originalFilename);
        storedFile.setFileType(file.getContentType());
        storedFile.setFileSize(file.getSize());
        storedFile.setStorageKey(key);
        storedFile.setBucketName(r2Bucket);
        storedFileRepository.save(storedFile);
    }
}
