package com.app.civify.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory cache service implementation (fallback for Redis)
 * WARNING: This is for development/testing only. In production, use Redis HA (cluster/sentinel)
 */
@Service
@Slf4j
public class InMemoryCacheService implements CacheService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public void set(String key, String value, Duration ttl) {
        Instant expiry = Instant.now().plus(ttl);
        cache.put(key, new CacheEntry(value, expiry));
        log.debug("IN_MEMORY cache SET: {} (TTL: {})", key, ttl);
    }

    @Override
    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("IN_MEMORY cache EXPIRED: {}", key);
            return null;
        }
        
        return entry.value;
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
        counters.remove(key);
        log.debug("IN_MEMORY cache DELETE: {}", key);
    }

    @Override
    public Long increment(String key) {
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        long value = counter.incrementAndGet();
        log.debug("IN_MEMORY cache INCREMENT: {} -> {}", key, value);
        return value;
    }

    @Override
    public void expire(String key, Duration ttl) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            Instant newExpiry = Instant.now().plus(ttl);
            cache.put(key, new CacheEntry(entry.value, newExpiry));
            log.debug("IN_MEMORY cache EXPIRE: {} (TTL: {})", key, ttl);
        }
    }

    @Override
    public boolean isRedisAvailable() {
        return false; // Always using in-memory
    }

    @Override
    public String getCacheMode() {
        return "IN_MEMORY";
    }

    /**
     * Cleanup expired entries every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredEntries() {
        int removed = 0;
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("IN_MEMORY cache cleanup: removed {} expired entries", removed);
        }
    }

    /**
     * Internal cache entry with expiry
     */
    private static class CacheEntry {
        final String value;
        final Instant expiry;

        CacheEntry(String value, Instant expiry) {
            this.value = value;
            this.expiry = expiry;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }
}
