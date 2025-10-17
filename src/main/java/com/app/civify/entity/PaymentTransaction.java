package com.app.civify.entity;

import jakarta.persistence.*;
import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String providerId;
    private String status;
    private double amount;
    private Instant createdAt;
}
