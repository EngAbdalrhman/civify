package com.app.civify.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * On deploy: remove legacy publicBlogs* Spring Cache keys written with an
 * incompatible Jackson format (caused HTTP 500 on /apis/v1/blogs).
 * <p>
 * Only deletes keys matching {@code *publicBlogs*} — safe when Redis is shared
 * between staging and production (never use FLUSHALL). See docs/BLOG_REDIS.md.
 */
@Component
@Slf4j
@ConditionalOnProperty(value = "cache.redis.enabled", havingValue = "true", matchIfMissing = true)
public class BlogCachePurgeRunner implements ApplicationRunner {

    private final StringRedisTemplate redis;

    public BlogCachePurgeRunner(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match("*publicBlogs*").count(100).build();
            try (Cursor<String> cursor = redis.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            if (!keys.isEmpty()) {
                Long removed = redis.delete(keys);
                log.info("Purged {} legacy publicBlogs Redis cache key(s) on startup", removed);
            }
        } catch (Exception e) {
            log.warn("Could not purge legacy publicBlogs cache keys (non-fatal): {}", e.getMessage());
        }
    }
}
