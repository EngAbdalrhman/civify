package com.app.civify.repository;

import com.app.civify.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {
    
    Optional<License> findByKey(String key);
    
    Optional<License> findByKeyAndIsActiveTrue(String key);
}
