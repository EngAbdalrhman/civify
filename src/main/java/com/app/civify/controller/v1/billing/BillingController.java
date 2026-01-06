package com.app.civify.controller.v1.billing;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.billing.InvoiceDto;
import com.app.civify.entity.billing.Invoice;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.service.billing.InvoiceService;
import com.app.civify.util.filter.InputDataModel;
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
import java.util.stream.Collectors;

@ApiV1Controller
@RequestMapping("/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Invoice and billing management")
@Slf4j
public class BillingController {

    private final InvoiceService invoiceService;

    @GetMapping("/invoices")
    @Operation(summary = "Get user's invoice history")
    public ResponseEntity<List<InvoiceDto>> getUserInvoices(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<Invoice> invoices = invoiceService.getUserInvoices(userId);
        List<InvoiceDto> dtos = invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/invoices/{uuid}")
    @Operation(summary = "Get invoice details by UUID (for checkout page)")
    public ResponseEntity<InvoiceDto> getInvoiceByUuid(@PathVariable UUID uuid) {
        Invoice invoice = invoiceService.getByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + uuid));
        return ResponseEntity.ok(toDto(invoice));
    }

    @PostMapping("/invoices/{uuid}/pay")
    @Operation(summary = "Generate payment link for invoice")
    public ResponseEntity<InvoiceDto> processPayment(
            @PathVariable UUID uuid,
            @RequestParam String gateway) {
        Invoice invoice = invoiceService.processPayment(uuid, gateway);
        return ResponseEntity.ok(toDto(invoice));
    }

    private InvoiceDto toDto(Invoice invoice) {
        return InvoiceDto.builder()
                .id(invoice.getId())
                .uuid(invoice.getUuid())
                .planName(invoice.getPlanName())
                .originalAmount(invoice.getOriginalAmount())
                .discountAmount(invoice.getDiscountAmount())
                .finalAmount(invoice.getFinalAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus().name())
                .billingType(invoice.getBillingType())
                .description(invoice.getDescription())
                .gateway(invoice.getGateway())
                .paymentUrl(invoice.getPaymentUrl())
                .dueDate(invoice.getDueDate())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
