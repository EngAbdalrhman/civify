package com.app.civify.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PaymentGatewayFactory {


    private final Map<String, PaymentGateway> gateways;
    private final PaymentGateway nullGateway = new NullPaymentGateway();

    @Value("${payment.gateway.default:fawaterak}")
    private String defaultGateway;

    public PaymentGatewayFactory(
            List<PaymentGateway> gatewayList
    ) {
        this.gateways = gatewayList.stream()
                .filter(PaymentGateway::isEnabled)
                .collect(Collectors.toMap(
                        PaymentGateway::getGatewayName,
                        Function.identity()
                ));

        log.info("Available payment gateways: {}", gateways.keySet());
    }

    public PaymentGateway get(String gatewayName) {
        if (gatewayName == null || gatewayName.isBlank()) {
            return getDefault();
        }
        return gateways.getOrDefault(gatewayName, nullGateway);
    }

    public PaymentGateway getDefault() {
        return gateways.getOrDefault(
                defaultGateway,
                nullGateway
        );
    }

    public List<String> getEnabledGateways() {
        return List.copyOf(gateways.keySet());
    }

    public boolean isAvailable(String gatewayName) {
        return gateways.containsKey(gatewayName);
    }

}
