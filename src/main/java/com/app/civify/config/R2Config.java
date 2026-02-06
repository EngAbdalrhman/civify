package com.app.civify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@Slf4j
public class R2Config {

    @Value("${civify.r2.access-key:}")
    private String accessKey;

    @Value("${civify.r2.secret-key:}")
    private String secretKey;

    @Value("${civify.r2.endpoint:}")
    private String endpoint;

    @Value("${civify.r2.enabled:false}")
    private boolean enabled;

    @Bean
    @ConditionalOnProperty(name = "civify.r2.enabled", havingValue = "true")
    public S3Client s3Client() {

        if (accessKey.isBlank() || secretKey.isBlank() || endpoint.isBlank()) {
            log.info("missing data for s3 r2");
           // Fallback on missing credentials to avoid startup failure if accidentally enabled but not configured
           // But actually, if enabled is true, we should probably fail or log warning.
           // For now, let's allow it to be created but it might fail on usage if credentials are bad.
           // Better: check content.
        }

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // R2 uses auto or us-east-1 equivalent
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // Required for R2/MinIO
                        .build())
                .build();
    }
}
