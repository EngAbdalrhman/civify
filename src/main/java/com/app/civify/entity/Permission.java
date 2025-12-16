package com.app.civify.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String category; // e.g., "CV", "API", "ADMIN"

    @ManyToMany(mappedBy = "permissions")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<Group> groups;
}