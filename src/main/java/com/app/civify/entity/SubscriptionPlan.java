package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionPlan extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    private String description;
    private String details;

    // Features
    private Integer cvLimit;
    private Integer apiLimit;
    private Boolean watermark;
    private Boolean dashboard;

    // Pricing
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;

    @Column(nullable = false)
    private Boolean active = true;

    // Subscription period in months (1 for monthly, 12 for annual)
    private Integer subscriptionPeriod;
}
