package com.app.civify.controller.general;

import com.app.civify.entity.security.ApiKey;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<?> createApiKey(@AuthenticationPrincipal UserPrincipal userDetails) {
        String rawKey = apiKeyService.createApiKey(userDetails.getUser());
        return ResponseEntity.ok(Map.of("key", rawKey));
    }

    @GetMapping
    public ResponseEntity<List<ApiKey>> getUserApiKeys(@AuthenticationPrincipal UserPrincipal userDetails) {
        return ResponseEntity.ok(apiKeyService.getUserApiKeys(userDetails.getUser()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeApiKey(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal userDetails) {
        apiKeyService.revokeApiKey(id, userDetails.getUser());
        return ResponseEntity.ok().build();
    }
}
