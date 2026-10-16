package com.app.civify.service;

import com.app.civify.service.cache.InMemoryCacheService;
import com.app.civify.service.cache.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpLinkCacheTest {
    @Test void inMemoryCountersHonorExpiryInsteadOfBlockingFutureLinkAttemptsForever() {
        var cache = new InMemoryCacheService();
        assertEquals(1L, cache.increment("rate"));
        assertEquals(2L, cache.increment("rate"));
        cache.expire("rate", Duration.ofSeconds(-1));
        assertEquals(1L, cache.increment("rate"));
        cache.set("expired-code", "fixture", Duration.ofSeconds(-1));
        assertNull(cache.getAndDelete("expired-code"));
    }
    @Test @SuppressWarnings("unchecked") void uncertainRedisConsumeNeverFallsBackToAnotherStore() {
        var redis = mock(StringRedisTemplate.class);
        var factory = mock(RedisConnectionFactory.class);
        when(redis.getConnectionFactory()).thenReturn(factory);
        when(factory.getConnection()).thenReturn(mock(RedisConnection.class));
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.getAndDelete("code-hash")).thenThrow(new IllegalStateException("fixture Redis failure"));
        var fallback = mock(InMemoryCacheService.class);
        var cache = new RedisCacheService(redis, fallback);
        assertThrows(IllegalStateException.class, () -> cache.getAndDelete("code-hash"));
        verifyNoInteractions(fallback);
    }

    @Test void concurrentCounterIncrementsReturnDistinctCounts() {
        var cache = new InMemoryCacheService();
        assertEquals(100, java.util.stream.IntStream.range(0, 100).parallel()
                .mapToObj(i -> cache.increment("rate")).distinct().count());
    }
}
