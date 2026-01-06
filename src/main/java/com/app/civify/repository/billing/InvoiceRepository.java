package com.app.civify.repository.billing;

import com.app.civify.entity.billing.Invoice;
import com.app.civify.entity.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByUuid(UUID uuid);

    Optional<Invoice> findByPaymentTransactionId(String paymentTransactionId);

    List<Invoice> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Invoice> findBySubscriptionIdAndStatus(Long subscriptionId, InvoiceStatus status);

    List<Invoice> findBySubscriptionIdAndStatusIn(Long subscriptionId, List<InvoiceStatus> statuses);

    @Modifying
    @Query("UPDATE Invoice i SET i.status = :status, i.expiredAt = CURRENT_TIMESTAMP WHERE i.subscription.id = :subscriptionId AND i.status = 'PENDING'")
    int expirePendingInvoicesBySubscriptionId(@Param("subscriptionId") Long subscriptionId, @Param("status") InvoiceStatus status);

    @Modifying
    @Query("UPDATE Invoice i SET i.status = 'CANCELLED' WHERE i.subscription.id = :subscriptionId AND i.status = 'PENDING'")
    int cancelPendingInvoicesBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.user.id = :userId AND i.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") InvoiceStatus status);
}
