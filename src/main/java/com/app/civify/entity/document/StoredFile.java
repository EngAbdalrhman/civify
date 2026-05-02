package com.app.civify.entity.document;

import com.app.civify.entity.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "stored_files")
@EqualsAndHashCode(callSuper = true)
public class StoredFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String fileName;

    private String fileType;

    private Long fileSize;

    @Column(nullable = false)
    private String storageKey; // R2 Path: cvs/date/filename

    @Column(nullable = false)
    private String bucketName;

    @Column(length = 64)
    private String fileHash;

    private LocalDateTime uploadDate;

    @Override
    public void prePersist() {
        super.prePersist();
        if (uploadDate == null) {
            uploadDate = LocalDateTime.now();
        }
    }
}
