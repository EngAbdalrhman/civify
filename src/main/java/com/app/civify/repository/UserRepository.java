package com.app.civify.repository;

import com.app.civify.entity.User;
import com.app.civify.entity.enums.UserPreferredLanguage;
import com.app.civify.entity.enums.UserPreferredTheme;
import com.app.civify.util.persistence.CommonRepo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CommonRepo<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = false")
    Optional<User> findActiveByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false")
    Optional<User> findActiveByEmail(@Param("email") String email);

    Optional<User> findByReferralCode(String referralCode);

    /**
     * Get only extraTokens (wallet balance) for a user.
     * Optimized query - avoids loading the entire User entity.
     */
    @Query("SELECT u.extraTokens FROM User u WHERE u.id = :userId")
    Optional<Integer> findExtraTokensByUserId(@Param("userId") Long userId);

    /**
     * Update extraTokens directly with a single UPDATE query.
     * Returns the number of rows affected.
     */
    @Modifying
    @Query("UPDATE User u SET u.extraTokens = :tokens WHERE u.id = :userId")
    int updateExtraTokens(@Param("userId") Long userId, @Param("tokens") Integer tokens);

    /**
     * Find users who need welcome emails (created today and haven't received one yet).
     */
    @Query("SELECT u FROM User u WHERE u.welcomeEmailSent = false AND u.isDeleted = false AND u.isEmailVerified = true")
    java.util.List<User> findUsersNeedingWelcomeEmail();

    /**
     * Update welcome email sent status for a user.
     */
    @Modifying
    @Query("UPDATE User u SET u.welcomeEmailSent = true WHERE u.id = :userId")
    int updateWelcomeEmailSent(@Param("userId") Long userId);

    /**
     * Update user preferences (Language & Theme) directly.
     * Bypasses EntityListeners (Audit).
     */
    @Modifying
    @Query("UPDATE User u SET u.preferredLanguage = :language, u.preferredTheme = :theme WHERE u.id = :userId")
    void updateUserPreferences(@Param("userId") Long userId,
                               @Param("language") UserPreferredLanguage language,
                               @Param("theme") UserPreferredTheme theme);

    /**
     * Update user onboarding completion status and tours.
     * Bypasses EntityListeners (Audit).
     */
    @Modifying
    @Query("UPDATE User u SET u.onboardingCompleted = :onboardingCompleted, u.completedTours = :completedTours, u.primaryIntent = :primaryIntent WHERE u.id = :userId")
    void updateUserOnboarding(@Param("userId") Long userId,
                              @Param("onboardingCompleted") Boolean onboardingCompleted,
                              @Param("completedTours") String completedTours,
                              @Param("primaryIntent") String primaryIntent);

    @Query("SELECT u FROM User u WHERE u.status = com.app.civify.entity.enums.UserStatus.ACTIVE AND u.isDeleted = false")
    List<User> findAllActiveUsers();
}
