package com.app.civify.repository.document;

import com.app.civify.entity.document.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findByUserId(Long userId);
    Optional<StoredFile> findByStorageKey(String storageKey);
    Optional<StoredFile> findFirstByFileHash(String fileHash);
}
