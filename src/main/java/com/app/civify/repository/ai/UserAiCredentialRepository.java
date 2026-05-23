package com.app.civify.repository.ai;

import com.app.civify.entity.ai.UserAiCredential;
import com.app.civify.entity.enums.AiCredentialStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAiCredentialRepository extends JpaRepository<UserAiCredential, UUID> {

    List<UserAiCredential> findByUserIdAndStatus(Long userId, AiCredentialStatus status);

    List<UserAiCredential> findByUserId(Long userId);

    Optional<UserAiCredential> findByIdAndUserId(UUID id, Long userId);
}
