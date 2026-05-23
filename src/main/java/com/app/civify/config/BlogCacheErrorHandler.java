package com.app.civify.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When Redis holds blog cache entries written with an older serializer/format,
 * reads throw SerializationException and the public API returned 500.
 * Evict the bad entry so the next request repopulates from the database.
 */
@Slf4j
public class BlogCacheErrorHandler implements CacheErrorHandler {

    private static final Duration WARN_THROTTLE = Duration.ofMinutes(15);
    private static final ConcurrentHashMap<String, Instant> lastWarnAt = new ConcurrentHashMap<>();

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        if (shouldLogWarn(cache.getName(), key)) {
            log.warn(
                    "Cache read failed for cache={} key={}: {} — evicting entry",
                    cache.getName(),
                    key,
                    exception.getMessage());
        }
        evictQuietly(cache, key);
        // Returning without rethrowing lets Spring treat this as a cache miss
        // and run the underlying service method.
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache put failed for cache={} key={}: {}", cache.getName(), key, exception.getMessage());
        evictQuietly(cache, key);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache evict failed for cache={} key={}: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache clear failed for cache={}: {}", cache.getName(), exception.getMessage());
    }

    private void evictQuietly(Cache cache, Object key) {
        try {
            if (key != null) {
                cache.evict(key);
            } else {
                cache.clear();
            }
        } catch (RuntimeException evictEx) {
            if (exceptionIndicatesCorruptEntry(evictEx)) {
                log.warn("Could not evict cache={} key={} — entry may already be gone", cache.getName(), key);
            } else {
                log.warn("Could not evict cache={} key={}", cache.getName(), key, evictEx);
            }
        }
    }

    private boolean exceptionIndicatesCorruptEntry(RuntimeException ex) {
        return ex instanceof SerializationException
                || (ex.getCause() != null && ex.getCause() instanceof SerializationException);
    }

    private boolean shouldLogWarn(String cacheName, Object key) {
        String throttleKey = cacheName + "::" + key;
        Instant now = Instant.now();
        Instant previous = lastWarnAt.put(throttleKey, now);
        return previous == null || previous.plus(WARN_THROTTLE).isBefore(now);
    }
}
