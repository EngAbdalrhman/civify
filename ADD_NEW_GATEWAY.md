# Adding a New Payment Gateway

This guide outlines the steps to add a new payment gateway to the platform, following our SaaS best practices.

## 1. Create Gateway Implementation

Create a new class implementing the `PaymentGateway` interface in `com.app.civify.service.payment`.

```java
@Component
@RequiredArgsConstructor
public class NewPaymentGateway implements PaymentGateway {
    
    @Value("${payment.gateway.newgateway.enabled:false}")
    private boolean enabled;

    @Override
    public String getGatewayName() {
        return "newgateway";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    // Implement other methods...
}
```

## 2. Update Configuration

Add configuration properties to `application.properties`.

```properties
# New Gateway Config
payment.gateway.newgateway.enabled=false
# Add any specific keys
payment.newgateway.api-key=${NEW_GATEWAY_API_KEY}
```

## 3. Register Bean

Ensure your new class is annotated with `@Component`. The `PaymentGatewayFactory` will automatically pick it up if it implements the `PaymentGateway` interface.

## 4. Environment Variables

If the gateway requires secrets (API keys), add them to `.env` or your deployment secrets management, and reference them in `application.properties`.

## Best Practices

- **Feature Flagging**: Always wrap functionality in `isEnabled()`.
- **Secrets Management**: Never commit API keys. Use environment variables.
- **Webhook Security**: Implement signature verification for callbacks.
