package com.app.civify.entity.plans;

import com.app.civify.entity.User;
import com.app.civify.entity.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "token_purchases")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenPurchase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private TokenPackage tokenPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer tokens;

    /**
     * Link to the payment transaction/order
     */
    @Column(columnDefinition = "TEXT")
    private String paymentTransactionId; // The 'orderId' or 'paymentKey' from gateway

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenPurchaseStatus status;

    private LocalDateTime completedAt;

    public enum TokenPurchaseStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
