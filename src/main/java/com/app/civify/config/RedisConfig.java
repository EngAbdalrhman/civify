package com.app.civify.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {
// Spring Boot auto-configures Redis when spring.redis.* props are set
}
