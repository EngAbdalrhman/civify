package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages multiple payment gateways using strategy pattern
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayManager {
    private final PaymentGatewayFactory gatewayFactory;

    /**
     * Get a payment gateway by name
     */
    public PaymentGateway getGateway(String gatewayName) {
        return gatewayFactory.get(gatewayName);
    }

    /**
     * Get the default payment gateway
     */
    public PaymentGateway getDefaultGateway() {
        return gatewayFactory.getDefault();
    }

    /**
     * Get all registered gateways
     */
    public List<PaymentGateway> getAllGateways() {
        return gatewayFactory.getEnabledGateways()
                .stream()
                .map(gatewayFactory::get)
                .toList();
    }

    /**
     * Check if a gateway is available
     */
    public boolean isGatewayAvailable(String gatewayName) {
        return gatewayFactory.isAvailable(gatewayName);
    }

    /**
     * Select a compatible payment gateway for the specified currency
     * Automatically finds the first enabled gateway that supports the currency
     *
     * @param currency ISO 4217 currency code (e.g., "EGP", "USD")
     * @return PaymentGateway that supports the currency
     * @throws RuntimeException if no compatible gateway is found
     */
    public PaymentGateway selectGatewayForCurrency(String currency) {
        if (currency == null || currency.isEmpty()) {
            log.warn("Currency is null or empty, using default gateway");
            return getDefaultGateway();
        }

        String currencyUpper = currency.toUpperCase();
        log.debug("Selecting gateway for currency: {}", currencyUpper);

        // Get all enabled gateways and find first that supports the currency
        List<PaymentGateway> gateways = getAllGateways();
        
        for (PaymentGateway gateway : gateways) {
            if (gateway.isEnabled() && gateway.getSupportedCurrencies().contains(currencyUpper)) {
                log.info("Selected gateway '{}' for currency {}", gateway.getGatewayName(), currencyUpper);
                return gateway;
            }
        }

        // No compatible gateway found
        List<String> enabledGateways = gatewayFactory.getEnabledGateways();
        log.error("No compatible gateway found for currency {}. Enabled gateways: {}", 
                  currencyUpper, enabledGateways);
        
        throw new RuntimeException(String.format(
            "Currency %s is not supported by any enabled payment gateway. " +
            "Please contact support or use a different currency.", 
            currencyUpper
        ));
    }

    /**
     * Validate that a specific gateway supports a currency
     *
     * @param gatewayName Name of the gateway
     * @param currency    Currency code
     * @return true if gateway supports the currency
     */
    public boolean validateGatewayCurrencySupport(String gatewayName, String currency) {
        if (gatewayName == null || currency == null) {
            return false;
        }
        
        try {
            PaymentGateway gateway = gatewayFactory.get(gatewayName);
            return gateway.isEnabled() && 
                   gateway.getSupportedCurrencies().contains(currency.toUpperCase());
        } catch (Exception e) {
            log.warn("Error validating gateway {} for currency {}: {}", 
                     gatewayName, currency, e.getMessage());
            return false;
        }
    }

    /**
     * Create payment using default gateway
     */
    public PaymentCreationResult createPayment(
            Long userId,
            double amount,
            String currency,
            Map<String, Object> metadata
    ) {
        return gatewayFactory
                .getDefault()
                .createPayment(userId, amount, currency, metadata);
    }

    /**
     * Create payment using specific gateway
     */
    public PaymentCreationResult createPayment(
            String gatewayName,
            Long userId,
            double amount,
            String currency,
            Map<String, Object> metadata
    ) {
        return gatewayFactory
                .get(gatewayName)
                .createPayment(userId, amount, currency, metadata);
    }

    /**
     * Verify payment using default gateway
     */
    public boolean verifyPayment(String transactionId) {
        return gatewayFactory
                .getDefault()
                .verifyPayment(transactionId);
    }

    /**
     * Handle callback using specific gateway
     */
    public PaymentCallbackResult handleCallback(
            String gatewayName,
            String rawPayload,
            Map<String, String> headers
    ) {
        return gatewayFactory
                .get(gatewayName)
                .handleCallback(rawPayload, headers);
    }

    /**
     * Get list of enabled gateways
     */
    public List<String> getEnabledGateways() {
        return gatewayFactory.getEnabledGateways();
    }
}
