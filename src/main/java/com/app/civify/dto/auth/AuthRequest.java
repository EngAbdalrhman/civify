package com.app.civify.dto.auth;

import lombok.Data;

@Data
public class AuthRequest {

    private String username;
    private String email;
    private String password;
}
