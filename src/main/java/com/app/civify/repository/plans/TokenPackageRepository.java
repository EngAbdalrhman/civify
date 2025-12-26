package com.app.civify.repository.plans;

import com.app.civify.entity.plans.TokenPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokenPackageRepository extends JpaRepository<TokenPackage, Long> {
    List<TokenPackage> findByActiveTrue();
}
