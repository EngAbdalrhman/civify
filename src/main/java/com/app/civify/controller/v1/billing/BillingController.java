package com.app.civify.controller.v1.billing;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.billing.InvoiceDto;
import com.app.civify.usecase.user.BillingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@ApiV1Controller
@RequestMapping("/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Invoice and billing management")
@Slf4j
public class BillingController {

    private final BillingUseCase billingUseCase;

    @GetMapping("/invoices")
    @Operation(summary = "Get user's invoice history")
    public ResponseEntity<List<InvoiceDto>> getUserInvoices(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(billingUseCase.getUserInvoices(userId));
    }

    @GetMapping("/invoices/{uuid}")
    @Operation(summary = "Get invoice details by UUID (for checkout page)")
    public ResponseEntity<InvoiceDto> getInvoiceByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(billingUseCase.getInvoiceByUuid(uuid));
    }

    @PostMapping("/invoices/{uuid}/pay")
    @Operation(summary = "Generate payment link for invoice")
    public ResponseEntity<InvoiceDto> processPayment(
            @PathVariable UUID uuid,
            @RequestParam String gateway) {
        return ResponseEntity.ok(billingUseCase.processPayment(uuid, gateway));
    }
}
