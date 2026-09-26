package com.app.civify.service.cache;

import java.time.Duration;

/**
 * Cache service abstraction that supports fallback to in-memory cache when Redis is unavailable
 */
public interface CacheService {

    /**
     * Set a value in cache with TTL
     * @param key Cache key
     * @param value Value to store
     * @param ttl Time to live
     */
    void set(String key, String value, Duration ttl);

    /**
     * Get a value from cache
     * @param key Cache key
     * @return Cached value or null if not found/expired
     */
    String get(String key);

    /** Atomically remove and return a value. Security codes must never use get + delete. */
    String getAndDelete(String key);

    /**
     * Delete a value from cache
     * @param key Cache key
     */
    void delete(String key);

    /**
     * Increment a counter
     * @param key Cache key
     * @return New value after increment
     */
    Long increment(String key);

    /**
     * Set expiry for a key
     * @param key Cache key
     * @param ttl Time to live
     */
    void expire(String key, Duration ttl);

    /**
     * Check if Redis is available
     * @return true if Redis is connected, false if using fallback
     */
    boolean isRedisAvailable();

    /**
     * Get current cache mode  
     * @return "REDIS" or "IN_MEMORY"
     */
    String getCacheMode();
}
