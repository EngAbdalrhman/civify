package com.app.civify.repository.jobmatch;

import com.app.civify.entity.jobmatch.JobMatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobMatchHistoryRepository extends JpaRepository<JobMatchHistory, Long> {
    
    Page<JobMatchHistory> findByUserIdOrderByAnalyzedAtDesc(Long userId, Pageable pageable);
    
    long countByUserId(Long userId);
}
