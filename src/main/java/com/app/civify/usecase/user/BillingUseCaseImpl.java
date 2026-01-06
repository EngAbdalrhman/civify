package com.app.civify.usecase.user;

import com.app.civify.dto.billing.InvoiceDto;
import com.app.civify.entity.billing.Invoice;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.service.billing.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingUseCaseImpl implements BillingUseCase {

    private final InvoiceService invoiceService;

    @Override
    public List<InvoiceDto> getUserInvoices(Long userId) {
        log.debug("Fetching invoices for user {}", userId);
        return invoiceService.getUserInvoices(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public InvoiceDto getInvoiceByUuid(UUID uuid) {
        log.debug("Fetching invoice by UUID: {}", uuid);
        Invoice invoice = invoiceService.getByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + uuid));
        return toDto(invoice);
    }

    @Override
    public InvoiceDto processPayment(UUID uuid, String gateway) {
        log.debug("Processing payment for invoice {} via {}", uuid, gateway);
        Invoice invoice = invoiceService.processPayment(uuid, gateway);
        return toDto(invoice);
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
