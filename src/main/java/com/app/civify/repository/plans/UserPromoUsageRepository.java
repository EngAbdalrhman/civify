package com.app.civify.repository.plans;

import com.app.civify.entity.plans.UserPromoUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPromoUsageRepository extends JpaRepository<UserPromoUsage, Long> {

    /**
     * Check if a user has already used a specific promo code.
     */
    boolean existsByUserIdAndPromoCodeId(Long userId, Long promoCodeId);

    /**
     * Find usage record for a user and promo code.
     */
    Optional<UserPromoUsage> findByUserIdAndPromoCodeId(Long userId, Long promoCodeId);
}
