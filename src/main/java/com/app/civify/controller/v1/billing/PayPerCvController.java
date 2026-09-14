package com.app.civify.controller.v1.billing;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.dto.payment.PayPerCvPricingDto;
import com.app.civify.dto.payment.PayPerCvPurchaseRequest;
import com.app.civify.entity.billing.CvEntitlement;
import com.app.civify.entity.enums.TransactionType;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.billing.CvEntitlementService;
import com.app.civify.service.payment.CurrencyService;
import com.app.civify.service.payment.GatewayRegionResolver;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.util.CommonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@ApiV1Controller
@RequestMapping("/pay-per-cv")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pay-Per-CV", description = "One-time CV unlock pass endpoints")
public class PayPerCvController {

    private final CvEntitlementService entitlementService;
    private final PaymentService paymentService;
    private final GatewayRegionResolver regionResolver;
    private final CurrencyService currencyService;
    private final ObjectMapper objectMapper;

    @Value("${payment.pay-per-cv.single-price-usd:2.99}")
    private BigDecimal singlePriceUsd;

    @Value("${payment.pay-per-cv.pack3-price-usd:6.99}")
    private BigDecimal pack3PriceUsd;

    @Operation(summary = "Get Pay-Per-CV localized pricing")
    @GetMapping("/pricing")
    public ResponseEntity<CommonResponse<List<PayPerCvPricingDto>>> getPricing(HttpServletRequest request) {
        String resolvedGateway = regionResolver.resolveGateway(request);
        String userCurrency = currencyService.getGatewayCurrency(resolvedGateway);
        BigDecimal rate = currencyService.getUsdToEgpRate();

        List<PayPerCvPricingDto> pricingList = new ArrayList<>();

        // 1. Single CV Pass
        BigDecimal singleEgp = singlePriceUsd.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        pricingList.add(PayPerCvPricingDto.builder()
                .productId("PAY_PER_CV_SINGLE")
                .name("Single CV Pass")
                .description("30 days full access to this tailored CV. Unlimited edits, watermark removed, PDF & DOCX export.")
                .basePriceUsd(singlePriceUsd)
                .priceEgp(singleEgp)
                .exchangeRate(rate)
                .userCurrency(userCurrency)
                .formattedDisplayPrice("EGP".equalsIgnoreCase(userCurrency) ? singleEgp + " EGP" : "$" + singlePriceUsd + " USD")
                .formattedSecondaryPrice("EGP".equalsIgnoreCase(userCurrency) ? "≈ $" + singlePriceUsd + " USD" : "≈ " + singleEgp + " EGP")
                .credits(1)
                .durationDays(30)
                .build());

        // 2. 3-CV Pack
        BigDecimal pack3Egp = pack3PriceUsd.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        pricingList.add(PayPerCvPricingDto.builder()
                .productId("PAY_PER_CV_PACK3")
                .name("3-CV Application Pack")
                .description("Unlock 3 separate tailored resumes. Perfect for targeting multiple distinct job profiles.")
                .basePriceUsd(pack3PriceUsd)
                .priceEgp(pack3Egp)
                .exchangeRate(rate)
                .userCurrency(userCurrency)
                .formattedDisplayPrice("EGP".equalsIgnoreCase(userCurrency) ? pack3Egp + " EGP" : "$" + pack3PriceUsd + " USD")
                .formattedSecondaryPrice("EGP".equalsIgnoreCase(userCurrency) ? "≈ $" + pack3PriceUsd + " USD" : "≈ " + pack3Egp + " EGP")
                .credits(3)
                .durationDays(30)
                .build());

        return ResponseEntity.ok(CommonResponse.success(pricingList));
    }

    @Operation(
            summary = "Purchase Pay-Per-CV pass",
            description = "Initiates checkout for a one-time CV pass. Binds to specific resumeId if provided.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("/purchase")
    public ResponseEntity<CommonResponse<PaymentCreationResult>> purchase(
            @RequestBody PayPerCvPurchaseRequest purchaseRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request) {

        Long userId = userPrincipal.getId();
        String resolvedGateway = purchaseRequest.getGateway();
        if (resolvedGateway == null || resolvedGateway.isBlank()) {
            resolvedGateway = regionResolver.resolveGateway(request);
        }

        boolean isPack = "PAY_PER_CV_PACK3".equalsIgnoreCase(purchaseRequest.getProductId());
        BigDecimal basePrice = isPack ? pack3PriceUsd : singlePriceUsd;
        int credits = isPack ? 3 : 1;

        BigDecimal amountToCharge = currencyService.convertForGateway(basePrice, resolvedGateway);
        PaymentMethods method = purchaseRequest.getMethod() != null ? purchaseRequest.getMethod() : PaymentMethods.CARD;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "PAY_PER_CV");
        metadata.put("productId", purchaseRequest.getProductId());
        metadata.put("resumeId", purchaseRequest.getResumeId());
        metadata.put("credits", credits);
        metadata.put("entitlementType", isPack ? "PACK_3" : "SINGLE_CV");
        if (purchaseRequest.getPhoneNumber() != null && !purchaseRequest.getPhoneNumber().isBlank()) {
            metadata.put("phoneNumber", purchaseRequest.getPhoneNumber());
        }
        if (purchaseRequest.getPromoCode() != null && !purchaseRequest.getPromoCode().isBlank()) {
            metadata.put("promoCode", purchaseRequest.getPromoCode());
        }

        log.info("Creating PAY_PER_CV payment for userId={} productId={} resumeId={} gateway={} amount={}",
                userId, purchaseRequest.getProductId(), purchaseRequest.getResumeId(), resolvedGateway, amountToCharge);

        PaymentCreationResult result = paymentService.createPayment(
                userId,
                amountToCharge.doubleValue(),
                resolvedGateway,
                method,
                TransactionType.PAY_PER_CV,
                metadata
        );

        return ResponseEntity.ok(CommonResponse.success(result));
    }

    @Operation(
            summary = "Get entitlement status for a specific resume",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/status")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getStatus(
            @RequestParam String resumeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long userId = userPrincipal.getId();
        Optional<CvEntitlement> activeOpt = entitlementService.getActiveEntitlement(userId, resumeId);

        Map<String, Object> response = new HashMap<>();
        if (activeOpt.isPresent()) {
            CvEntitlement ent = activeOpt.get();
            response.put("active", true);
            response.put("watermarkFree", ent.getWatermarkFree());
            response.put("allowDocExport", ent.getAllowDocExport());
            response.put("unlimitedEditsUntil", ent.getUnlimitedEditsUntil());
            response.put("entitlementType", ent.getEntitlementType());
        } else {
            response.put("active", false);
            response.put("watermarkFree", false);
            response.put("allowDocExport", false);
        }

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(
            summary = "Get all active entitlements for current user",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/my-entitlements")
    public ResponseEntity<CommonResponse<List<CvEntitlement>>> getMyEntitlements(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        return ResponseEntity.ok(CommonResponse.success(entitlementService.getUserEntitlements(userId)));
    }
}
