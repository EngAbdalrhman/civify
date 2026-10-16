package com.app.civify.controller.general;

import com.app.civify.security.UserPrincipal;
import com.app.civify.service.McpAccountLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/mcp/account-link")
@RequiredArgsConstructor
public class McpAccountLinkController {
    private final McpAccountLinkService service;
    public record Approval(String transaction) {}
    public record Exchange(String code, String verifier, String transaction) {}

    @GetMapping("/requests/{transaction}")
    public ResponseEntity<?> describe(@PathVariable String transaction, @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireLogin(principal, authorization);
        return response(service.describe(transaction, principal.getUser()));
    }
    @PostMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestBody Approval approval, @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireLogin(principal, authorization);
        return response(service.authorize(approval.transaction(), principal.getUser()));
    }
    @PostMapping("/exchange")
    public ResponseEntity<?> exchange(@RequestBody Exchange exchange) {
        return response(service.exchange(exchange.code(), exchange.verifier(), exchange.transaction()));
    }
    private static void requireLogin(UserPrincipal principal, String authorization) {
        // Reject cookie-only authentication: this endpoint requires an explicit web-session bearer token.
        if (principal == null || authorization == null || !authorization.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to Civify to connect your account.");
    }
    private static ResponseEntity<?> response(Object body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy", "no-referrer").body(body);
    }
}
