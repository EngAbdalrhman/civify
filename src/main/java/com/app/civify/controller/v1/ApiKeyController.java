package com.app.civify.controller.v1;

import com.app.civify.entity.ApiKey;
import com.app.civify.entity.User;
import com.app.civify.service.ApiKeyService;
import com.app.civify.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createApiKey(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser();
        String rawKey = apiKeyService.createApiKey(user);
        return ResponseEntity.ok(Map.of("key", rawKey));
    }

    @GetMapping
    public ResponseEntity<List<ApiKey>> getUserApiKeys(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(apiKeyService.getUserApiKeys(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeApiKey(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser();
        apiKeyService.revokeApiKey(id, user);
        return ResponseEntity.ok().build();
    }
}
