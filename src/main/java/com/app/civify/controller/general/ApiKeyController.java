package com.app.civify.controller.general;

import com.app.civify.entity.security.ApiKey;
import com.app.civify.entity.User;
import com.app.civify.service.ApiKeyService;
import com.app.civify.service.user.UserService;
import com.app.civify.util.security.EntityType;
import com.app.civify.util.security.IdEncodingUtil;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final IdEncodingUtil idEncodingUtil;

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
    public ResponseEntity<?> revokeApiKey(
            @Parameter(description = "Encoded API key ID") @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser();
        Long decodedId = idEncodingUtil.decode(id, EntityType.API_KEY);
        apiKeyService.revokeApiKey(decodedId, user);
        return ResponseEntity.ok().build();
    }
}

