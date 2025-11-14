package com.app.civify.config;

import com.app.civify.util.audit.UserAuditListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for audit logging
 */
@Configuration
public class AuditConfig {

    /**
     * Register UserAuditListener as a Spring bean
     * This allows it to use dependency injection
     */
    @Bean
    public UserAuditListener userAuditListener() {
        return new UserAuditListener();
    }
}

