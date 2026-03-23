package com.app.civify.service.referral;

import com.app.civify.dto.referral.ReferralStatsResponse;
import com.app.civify.entity.User;
import com.app.civify.exception.BadRequestException;
import com.app.civify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private final UserRepository userRepository;

    @Value("${referral.token.reward:100}")
    private int referralRewardTokens;

    /**
     * Generates a unique 8-character referral code for a user.
     */
    public String generateUniqueCode() {
        String code;
        boolean isUnique = false;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Optional<User> existing = userRepository.findByReferralCode(code);
            if (existing.isEmpty()) {
                isUnique = true;
            }
        } while (!isUnique);
        return code;
    }

    /**
     * Applies a referral code for a newly registered user.
     * Rewards both the referrer and the new user with extra tokens.
     */
    @Transactional
    public void applyReferralCode(User newReferralUser, String referralCode) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return;
        }

        // Prevent reapplying
        if (newReferralUser.getReferredByCode() != null) {
            log.warn("User {} already has a referredByCode. Cannot apply again.", newReferralUser.getId());
            return;
        }

        // Cannot refer yourself
        if (referralCode.equals(newReferralUser.getReferralCode())) {
            throw new BadRequestException("referral.cannot.refer.self");
        }

        // Find referring user
        Optional<User> referrerOpt = userRepository.findByReferralCode(referralCode);
        if (referrerOpt.isEmpty()) {
            // Invalid code, but we might just ignore or throw error. Let's throw an error so the user knows.
            throw new BadRequestException("referral.code.invalid");
        }

        User referrer = referrerOpt.get();

        // Update Referrer
        referrer.setReferralCount(referrer.getReferralCount() + 1);
        int updatedReferrerTokens = referrer.getExtraTokens() + referralRewardTokens;
        userRepository.updateExtraTokens(referrer.getId(), updatedReferrerTokens);
        userRepository.save(referrer); // to save referralCount

        // Update New User
        newReferralUser.setReferredByCode(referralCode);
        int updatedNewUserTokens = newReferralUser.getExtraTokens() + referralRewardTokens;
        userRepository.updateExtraTokens(newReferralUser.getId(), updatedNewUserTokens);
        userRepository.save(newReferralUser);

        log.info("Referral code {} applied successfully. Rewards given to User {} and User {}", 
                referralCode, referrer.getId(), newReferralUser.getId());
    }

    /**
     * Get real-time stats for the user's dashboard.
     */
    public ReferralStatsResponse getMyReferralStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("user.notfound"));

        // Ensure user has a referral code (for older accounts)
        if (user.getReferralCode() == null || user.getReferralCode().isEmpty()) {
            user.setReferralCode(generateUniqueCode());
            userRepository.save(user);
        }

        int count = user.getReferralCount() != null ? user.getReferralCount() : 0;

        return ReferralStatsResponse.builder()
                .referralCode(user.getReferralCode())
                .referralCount(count)
                .tokensEarnedFromReferrals(count * referralRewardTokens)
                .build();
    }
}
