package com.app.civify.controller.v1;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.dto.auth.AuthResponse;
import com.app.civify.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@ApiV1Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam("refreshToken") String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam("refreshToken") String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/enable")
    // @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> enable2fa(@RequestParam("username") String username) {
        authService.enable2fa(username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestParam("username") String username,
            @RequestParam("code") String code) {
        return ResponseEntity.ok(authService.verifyOtp(username, code));
    }
}
