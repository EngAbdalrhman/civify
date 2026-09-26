package com.app.civify.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;

/**
 * Redis-based cache service implementation
 * Automatically falls back to in-memory cache when Redis is unavailable
 * Marked as @Primary to be the default CacheService implementation
 */
@Service
@Primary
@Slf4j
@ConditionalOnProperty(value = "cache.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redis;
    private final InMemoryCacheService fallbackCache;
    private boolean redisAvailable = true;

    public RedisCacheService(StringRedisTemplate redis, InMemoryCacheService fallbackCache) {
        this.redis = redis;
        this.fallbackCache = fallbackCache;
        checkRedisConnection();
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        try {
            redis.opsForValue().set(key, value, ttl);
            redisAvailable = true;
        } catch (Exception e) {
            handleRedisFailure("set", key, e);
            fallbackCache.set(key, value, ttl);
        }
    }

    @Override
    public String get(String key) {
        try {
            String value = redis.opsForValue().get(key);
            redisAvailable = true;
            return value;
        } catch (Exception e) {
            handleRedisFailure("get", key, e);
            return fallbackCache.get(key);
        }
    }

    @Override
    public void delete(String key) {
        try {
            redis.delete(key);
            redisAvailable = true;
        } catch (Exception e) {
            handleRedisFailure("delete", key, e);
            fallbackCache.delete(key);
        }
    }

    @Override
    public String getAndDelete(String key) {
        // Fail closed on an uncertain Redis consume; switching stores could allow replay.
        return redis.opsForValue().getAndDelete(key);
    }

    @Override
    public Long increment(String key) {
        try {
            Long value = redis.opsForValue().increment(key);
            redisAvailable = true;
            return value;
        } catch (Exception e) {
            handleRedisFailure("increment", key, e);
            return fallbackCache.increment(key);
        }
    }

    @Override
    public void expire(String key, Duration ttl) {
        try {
            redis.expire(key, ttl);
            redisAvailable = true;
        } catch (Exception e) {
            handleRedisFailure("expire", key, e);
            fallbackCache.expire(key, ttl);
        }
    }

    @Override
    public boolean isRedisAvailable() {
        return redisAvailable;
    }

    @Override
    public String getCacheMode() {
        return redisAvailable ? "REDIS" : "IN_MEMORY";
    }

    private void checkRedisConnection() {
        try {
            redis.getConnectionFactory().getConnection().ping();
            redisAvailable = true;
            log.info("Redis connection established successfully");
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis connection failed, using in-memory fallback cache", e);
        }
    }

    private void handleRedisFailure(String operation, String key, Exception e) {
        if (redisAvailable) {
            // First failure - log error
            log.error("Redis {} operation failed for key: {}. Switching to fallback cache. Error: {}",
                    operation, key, e.getMessage());
            redisAvailable = false;
        } else {
            // Already in fallback mode - log debug only
            log.debug("Redis unavailable - using fallback cache for {} on key: {}", operation, key);
        }
    }
}
