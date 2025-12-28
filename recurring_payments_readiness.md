---
description: how to implement Fawaterak recurring payments when available
---

# Fawaterak Recurring Payments Readiness

This document outlines the steps to integrate Fawaterak's recurring payment functionality when it becomes available.

## Current Implementation

The current subscription renewal flow uses a **manual invoicing approach**:

1. **Quartz Job** (`SubscriptionJob.java`) runs daily and:
   - Finds subscriptions expiring tomorrow (with `autoRenew = true`)
   - Creates a one-time payment invoice via Fawaterak
   - Emails the payment link to the user
   
2. **User pays** the renewal invoice manually

3. **Webhook callback** (`FawaterakPaymentGateway.handleCallback`) processes the payment:
   - Updates payment transaction status
   - Extends subscription end date

## Preparation for Recurring Payments

When Fawaterak releases their recurring payment API, integrate as follows:

### Step 1: Update FawaterakClient

Add a new method to `FawaterakClient.java`:

```java
public SubscriptionCreateResponse createRecurringSubscription(
    RecurringSubscriptionRequest request
) {
    // Call Fawaterak's recurring subscription API
    // Store the subscription ID returned by Fawaterak
}
```

### Step 2: Store Fawaterak Subscription ID

Add a field to `UserSubscription.java`:

```java
@Column(name = "fawaterak_subscription_id")
private String fawaterakSubscriptionId;
```

### Step 3: Modify Initial Payment Flow

In `PaymentUseCaseImpl.java`, when creating a new subscription payment:

```java
// Instead of one-time payment, create recurring subscription
if (plan.isRecurringEnabled()) {
    SubscriptionCreateResponse fawaterakSub = fawaterakClient.createRecurringSubscription(...);
    userSubscription.setFawaterakSubscriptionId(fawaterakSub.getId());
}
```

### Step 4: Handle Recurring Webhook Events

Fawaterak will send webhooks for:
- `subscription.created` - Initial subscription created
- `subscription.renewed` - Automatic renewal successful
- `subscription.failed` - Renewal payment failed
- `subscription.cancelled` - User cancelled via Fawaterak

Add handling in `FawaterakPaymentGateway.handleCallback`:

```java
String eventType = extractEventType(payload);
switch (eventType) {
    case "subscription.renewed":
        extendSubscription(fawaterakSubscriptionId);
        break;
    case "subscription.failed":
        markSubscriptionPaymentFailed(fawaterakSubscriptionId);
        sendPaymentFailureEmail(userId);
        break;
    case "subscription.cancelled":
        cancelSubscription(fawaterakSubscriptionId);
        break;
}
```

### Step 5: Update Quartz Job

Once recurring payments are active, modify `SubscriptionJob.processInvoicing()`:

```java
// Skip invoicing for subscriptions managed by Fawaterak recurring
if (sub.getFawaterakSubscriptionId() != null) {
    log.debug("Skipping invoicing for Fawaterak-managed subscription {}", sub.getId());
    continue;
}
```

### Step 6: Implement Cancellation Sync

When user cancels via our app (sets `autoRenew = false`), sync with Fawaterak:

```java
public void cancelActiveSubscription(Long userId) {
    // ... existing logic ...
    
    // Cancel at Fawaterak if managed there
    if (subscription.getFawaterakSubscriptionId() != null) {
        fawaterakClient.cancelSubscription(subscription.getFawaterakSubscriptionId());
    }
}
```

## Testing Checklist

- [ ] Verify Fawaterak sandbox supports recurring payments
- [ ] Test initial subscription creation via Fawaterak recurring API
- [ ] Verify webhook signatures for new event types
- [ ] Test successful renewal webhook processing
- [ ] Test failed renewal handling (card expired, insufficient funds)
- [ ] Test cancellation sync (both directions)
- [ ] Verify grace period handling when Fawaterak retry logic is active

## Configuration Required

When Fawaterak releases the API, add these environment variables:

```properties
fawaterak.recurring.enabled=true
fawaterak.recurring.retry-attempts=3
fawaterak.recurring.grace-period-days=3
```

## Resources

- Fawaterak API Documentation: https://docs.fawaterk.com/
- Contact Fawaterak support to get notified when recurring payments launch
