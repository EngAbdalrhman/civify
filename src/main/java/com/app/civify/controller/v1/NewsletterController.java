package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.newsletter.NewsletterBroadcastRequest;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.NewsletterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@ApiV1Controller
@RequestMapping("/newsletter")
@RequiredArgsConstructor
@Tag(name = "Newsletter", description = "Newsletter subscription endpoints")
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to newsletter")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        newsletterService.subscribe(email);
        return ResponseEntity.ok(Map.of("message", "Subscribed successfully"));
    }

    @PostMapping("/broadcast")
    @CivifyAuthorization(permission = "NEWSLETTER.BROADCAST")
    @Operation(summary = "Broadcast newsletter to all subscribers", security = @SecurityRequirement(name = "bearer-key"))
    public ResponseEntity<?> broadcast(@Valid @RequestBody NewsletterBroadcastRequest request) {
        newsletterService.broadcastEmail(request.getSubject(), request.getContent(), request.isHtml());
        return ResponseEntity.ok(Map.of("message", "Broadcast initiated successfully"));
    }

    @GetMapping("/unsubscribe-link")
    @Operation(summary = "Unsubscribe via link (public)")
    public ResponseEntity<String> unsubscribeLink(@RequestParam("id") UUID id) {
        boolean success = newsletterService.unsubscribeById(id);
        if (success) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><h1>Unsubscribed</h1><p>You have been successfully unsubscribed from the newsletter.</p></body></html>");
        }
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_HTML)
                .body("<html><body><h1>Error</h1><p>Invalid unsubscribe link.</p></body></html>");
    }
}
