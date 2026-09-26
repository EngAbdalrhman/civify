package com.app.civify.service;

import com.app.civify.entity.User;
import com.app.civify.entity.security.ApiKey;
import com.app.civify.repository.security.ApiKeyRepository;
import com.app.civify.repository.logs.ApiKeyUsageLogRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

class ApiKeyAccountStatusTest {
    @Test void existingCredentialStopsWorkingWhenItsAccountIsDisabled() {
        var repository = mock(ApiKeyRepository.class);
        var service = new ApiKeyService(repository, mock(ApiKeyUsageLogRepository.class));
        var user = mock(User.class);
        var key = new ApiKey(); key.setUser(user); key.setIsActive(true);
        when(repository.findByKeyHash(anyString())).thenReturn(Optional.of(key));
        when(user.isEnabled()).thenReturn(true);
        assertSame(key, service.validateKey("cv-fy-fixture"));
        when(user.isEnabled()).thenReturn(false);
        assertNull(service.validateKey("cv-fy-fixture"));
    }
    @Test void revokedCredentialRemainsInvalidEvenForAnEnabledAccount() {
        var repository = mock(ApiKeyRepository.class);
        var service = new ApiKeyService(repository, mock(ApiKeyUsageLogRepository.class));
        var key = new ApiKey(); key.setIsActive(false);
        when(repository.findByKeyHash(anyString())).thenReturn(Optional.of(key));
        assertNull(service.validateKey("cv-fy-fixture"));
    }
}
