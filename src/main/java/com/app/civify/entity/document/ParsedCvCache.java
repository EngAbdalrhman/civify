package com.app.civify.entity.document;

import com.app.civify.entity.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "parsed_cv_cache")
@EqualsAndHashCode(callSuper = true)
public class ParsedCvCache extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String fileHash;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String parsedJson;

    @Column(length = 10)
    private String detectedLanguage;

    private Double confidence;

    @Column(length = 50)
    private String processingMethod;

    private Integer tokensUsed;
}
