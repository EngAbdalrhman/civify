package com.app.civify.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing plans with prices in user's detected currency
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlansWithCurrencyResponse {
    private CurrencyInfo currency;
    private List<PlanPriceDto> plans;
    private List<String> availableGateways;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencyInfo {
        private String code;
        private String symbol;
        private String name;
    }
}
