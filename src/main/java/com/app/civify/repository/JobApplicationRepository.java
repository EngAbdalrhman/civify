package com.app.civify.repository;

import com.app.civify.entity.resume.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    
    List<JobApplication> findByUserId(Long userId);
    
    List<JobApplication> findByUserIdAndApplicationStatus(Long userId, String status);
    
    List<JobApplication> findByCvId(String cvId);
}
