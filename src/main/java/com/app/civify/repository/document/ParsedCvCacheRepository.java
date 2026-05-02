package com.app.civify.repository.document;

import com.app.civify.entity.document.ParsedCvCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParsedCvCacheRepository extends JpaRepository<ParsedCvCache, Long> {
    Optional<ParsedCvCache> findByFileHash(String fileHash);
}
