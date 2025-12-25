package com.app.civify.repository;

import com.app.civify.entity.plans.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<PromoCode> {

    Optional<PromoCode> findByCodeAndActiveTrue(String code);
}
