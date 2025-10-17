package com.app.civify.security.model;

import lombok.Getter;
import java.util.Arrays;

@Getter
public enum Role {
    ROLE_ADMIN(0, "Administrator with full system access"),
    ROLE_LICENSED_USER(1, "Licensed user with premium features access"),
    ROLE_USER(2, "Regular user with basic access");

    private final int id;
    private final String description;

    Role(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public static Role fromId(int id) {
        return Arrays.stream(Role.values())
                .filter(role -> role.id == id)
                .findFirst()
                .orElse(null);
    }
}
