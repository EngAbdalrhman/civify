package com.app.civify.repository.security;

import com.app.civify.entity.security.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    List<ApiKey> findByUserId(Long userId);
    
    List<ApiKey> findByUserIdAndIsActiveTrue(Long userId);
    
    Optional<ApiKey> findByKeyHash(String keyHash);
    
    Optional<ApiKey> findByKeyHashAndIsActiveTrue(String keyHash);
}
