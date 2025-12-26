package com.app.civify.entity.plans;

import com.app.civify.entity.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "token_packages")
public class TokenPackage extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer tokens;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String currency = "EGP";

    private Boolean active = true;

    private String description;
}
