package com.app.civify.security.model;

import lombok.Getter;
import java.util.Arrays;

@Getter
public enum Role {
    ROLE_ADMIN(0),
    ROLE_LICENSED_USER(1),
    ROLE_USER(2);

    private final int id;

    Role(int id) {
        this.id = id;
    }

    public static Role fromId(int id) {
        return Arrays.stream(Role.values())
                .filter(role -> role.getId() == id)
                .findFirst()
                .orElse(null);
    }
}
