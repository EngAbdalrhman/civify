package com.app.civify.repository.billing;

import com.app.civify.entity.billing.CvEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CvEntitlementRepository extends JpaRepository<CvEntitlement, Long> {

    List<CvEntitlement> findByUserIdAndStatus(Long userId, String status);

    Optional<CvEntitlement> findByUserIdAndResumeIdAndStatus(Long userId, String resumeId, String status);

    Optional<CvEntitlement> findByOrderId(String orderId);

    @Query("SELECT COUNT(e) > 0 FROM CvEntitlement e " +
           "WHERE e.userId = :userId " +
           "AND e.resumeId = :resumeId " +
           "AND e.status = 'ACTIVE' " +
           "AND e.unlimitedEditsUntil > :now")
    boolean existsActiveForResume(@Param("userId") Long userId, 
                                 @Param("resumeId") String resumeId, 
                                 @Param("now") Instant now);

    @Query("SELECT e FROM CvEntitlement e " +
           "WHERE e.userId = :userId " +
           "AND (e.resumeId IS NULL OR e.resumeId = '') " +
           "AND e.status = 'ACTIVE' " +
           "AND e.creditsRemaining > 0 " +
           "AND e.unlimitedEditsUntil > :now")
    List<CvEntitlement> findUnassignedCredits(@Param("userId") Long userId, 
                                             @Param("now") Instant now);
}
