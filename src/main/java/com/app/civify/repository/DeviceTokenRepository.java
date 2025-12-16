package com.app.civify.repository;

import com.app.civify.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /**
     * Find all active tokens for a user
     */
    List<DeviceToken> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find all active tokens for all users (for broadcast)
     */
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.isActive = true")
    List<DeviceToken> findAllActiveTokens();

    /**
     * Find token by value
     */
    Optional<DeviceToken> findByToken(String token);

    /**
     * Deactivate token (mark as invalid)
     */
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.token = :token")
    void deactivateToken(@Param("token") String token);

    /**
     * Delete inactive tokens older than specified date
     */
    void deleteByIsActiveFalse();
}
