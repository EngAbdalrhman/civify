package com.app.civify.repository;

import com.app.civify.entity.PlanFeature;
import com.app.civify.entity.enums.FeatureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanFeatureRepository extends JpaRepository<PlanFeature, Long> {

    Optional<PlanFeature> findByFeatureType(FeatureType featureType);

    List<PlanFeature> findByActiveTrue();

    List<PlanFeature> findAllByOrderByDisplayOrderAsc();
}

