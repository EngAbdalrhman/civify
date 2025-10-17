package com.app.civify.entity;

import com.app.civify.security.model.Role;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false)
    private boolean twoFactorEnabled = false;
    @Column(name = "oauth_provider")
    private String oauthProvider; // e.g. "google"
    @Column(name = "oauth_subject")
    private String oauthSubject; // provider user id

}
