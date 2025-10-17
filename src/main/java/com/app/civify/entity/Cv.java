package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cvs")
@Getter
@Setter
public class Cv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fileName;
    private String fileId;
    @Lob
    private String rawText;
    @Lob
    private String structuredData;
}
