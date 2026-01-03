package com.app.civify.repository.resume;

import com.app.civify.entity.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, String> {
    List<Resume> findByUserId(Long userId);
    Optional<Resume> findByIdAndUserId(String id, Long userId);
    long countByUserId(Long userId);
}
