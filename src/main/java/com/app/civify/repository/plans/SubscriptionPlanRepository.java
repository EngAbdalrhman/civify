package com.app.civify.repository.plans;

import com.app.civify.entity.plans.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long>, JpaSpecificationExecutor<SubscriptionPlan> {

    List<SubscriptionPlan> findByActiveTrue();

    Optional<SubscriptionPlan> findByName(String name);
    
    List<SubscriptionPlan> findByIsDeletedFalse();
}
