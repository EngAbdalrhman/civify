package com.app.civify.repository.resume;

import com.app.civify.entity.resume.Cv;
import com.app.civify.entity.enums.CvGenerationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CvRepository extends JpaRepository<Cv, Long> {

    /**
     * Count total CVs stored for a user
     */
    long countByUserId(Long userId);

    /**
     * Count CVs by user and generation type
     */
    long countByUserIdAndGenerationType(Long userId, CvGenerationType generationType);

    /**
     * Find all CVs for a user
     */
    List<Cv> findByUserId(Long userId);

    /**
     * Find CVs by user and generation type
     */
    List<Cv> findByUserIdAndGenerationType(Long userId, CvGenerationType generationType);

    /**
     * Find CVs by user with cover letter
     */
    List<Cv> findByUserIdAndHasCoverLetterTrue(Long userId);

    /**
     * Find masked CVs by user
     */
    List<Cv> findByUserIdAndIsMaskedTrue(Long userId);
}
