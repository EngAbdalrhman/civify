package com.app.civify.service.auth;

import com.app.civify.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtensionOAuthCodeService {

    private static final String CODE_PREFIX = "ext:oauth:code:";
    private static final Duration CODE_TTL = Duration.ofMinutes(2);

    private final CacheService cacheService;

    public String issueCode(String accessToken) {
        String code = UUID.randomUUID().toString();
        cacheService.set(getCodeKey(code), accessToken, CODE_TTL);
        return code;
    }

    public String consumeCode(String code) {
        String key = getCodeKey(code);
        String accessToken = cacheService.get(key);
        if (accessToken != null) {
            cacheService.delete(key);
        }
        return accessToken;
    }

    private String getCodeKey(String code) {
        return CODE_PREFIX + code;
    }
}
