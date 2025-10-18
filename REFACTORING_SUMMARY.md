# Civify Backend Refactoring Summary

## Overview
This document summarizes the major refactoring completed on the Civify backend project to improve architecture, remove redundancy, and implement a robust subscription and payment system with Paymob integration.

---

## ✅ Completed Changes

### 1. **Architecture Improvements**

#### **Separated Enums into Individual Files**
- Created `src/main/java/com/app/civify/entity/enums/` package
- Extracted enums from entity classes:
  - `SubscriptionStatus.java` - ACTIVE, EXPIRED, CANCELLED, PENDING
  - `DiscountType.java` - PERCENTAGE, FIXED_AMOUNT
  - `PaymentStatus.java` - PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED

**Benefits**: Better code organization, reusability, and easier maintenance

---

### 2. **Entity Refactoring**

#### **User Entity** (`User.java`)
**Removed**:
- `apiUsageMonth`, `apiUsageCount` - redundant monthly API tracking
- `cvCount` - legacy CV counter
- `uiCvCount`, `uiCvMonth` - redundant monthly CV tracking
- All related increment/check methods (`incrementApiUsage`, `canUseApi`, etc.)

**Added**:
- `getActiveSubscription()` - helper method to get current active subscription

**Rationale**: Usage tracking now centralized in `UserSubscription` entity to avoid duplication and confusion.

#### **UserSubscription Entity** (`UserSubscription.java`)
**Added**:
- `usageResetDate` - automatic monthly usage reset tracking
- `resetUsageIfNewMonth()` - auto-resets counters on first use each month
- Enhanced `canUseCv()` and `canUseApi()` with automatic monthly reset

**Updated**:
- Uses `SubscriptionStatus` enum from separate file
- Improved documentation

#### **PaymentTransaction Entity** (`PaymentTransaction.java`)
**Changed**:
- `status`: String → `PaymentStatus` enum
- `amount`: double → `BigDecimal` (better precision)
- Added `currency` field with default "EGP"
- Added `subscriptionId` for better linking
- Changed `metadata` from JSONB to TEXT for database compatibility

#### **PromoCode Entity** (`PromoCode.java`)
**Updated**:
- Uses `DiscountType` enum from separate file
- Removed inline enum definition

---

### 3. **New DTOs for Better API Structure**

Created comprehensive DTOs in `src/main/java/com/app/civify/dto/`:

**Subscription DTOs**:
- `SubscriptionRequest.java` - Request to create subscription
- `SubscriptionResponse.java` - Detailed subscription info
- `PriceCalculationResponse.java` - Price breakdown with discounts

**Payment DTOs**:
- `PaymentRequest.java` - Payment initiation request
- `PaymentInitiationResponse.java` - Payment details with URL
- `PaymentResponse.java` - Payment status response

**Paymob DTOs** (`dto/payment/paymob/`):
- `PaymobAuthResponse.java` - Authentication response
- `PaymobOrderResponse.java` - Order creation response
- `PaymobPaymentKeyResponse.java` - Payment key response

---

### 4. **Complete Paymob Payment Implementation**

#### **PaymobPaymentGateway.java** - Fully Implemented
**No more placeholders!** Complete implementation includes:

**Payment Flow**:
1. `authenticate()` - Get auth token from Paymob API
2. `createOrder()` - Create order with amount
3. `createPaymentKey()` - Generate payment key for checkout
4. `getPaymentUrl()` - Build iframe URL for user payment

**Callback Handling**:
- `handleCallback()` - Process Paymob webhook
- `verifyPayment()` - Verify payment status
- Proper JSON parsing using ObjectMapper
- Support for nested callback data structures

**Features**:
- Proper error handling and logging
- BigDecimal for accurate money calculations
- Metadata storage for subscription info
- Flexible billing data from user info
- Currency support (EGP default)

**Configuration** (application.properties):
```properties
payment.paymob.base-url=https://accept.paymob.com
payment.paymob.api-key=${PAYMOB_API_KEY:your_key_here}
payment.paymob.integration-id=${PAYMOB_INTEGRATION_ID:your_id_here}
payment.paymob.iframe-id=${PAYMOB_IFRAME_ID:your_iframe_id}
payment.paymob.hmac-secret=${PAYMOB_HMAC_SECRET:}
```

---

### 5. **Subscription Service Refactoring - Option A Flow**

#### **SubscriptionServiceImpl.java** - Complete Rewrite

**Option A Implementation**: Single Active Subscription at a Time
- User registers → Automatically gets FREE plan (never expires - 100 years)
- User upgrades to paid plan → FREE plan becomes CANCELLED, paid plan becomes ACTIVE
- Paid plan expires → FREE plan automatically reactivates (via `reactivateFreePlan()`)
- **No manual downgrade needed!**

**Key Methods**:
- `createSubscription()` - Creates subscription with PENDING status
- `initiatePayment()` - Creates subscription + payment, links them
- `activateSubscription()` - Payment success → deactivate old subs, activate new one
- `reactivateFreePlan()` - Auto-reactivate free plan when paid expires
- `createDefaultFreeSubscription()` - Called on user registration
- `calculateTotalPrice()` - Returns `PriceCalculationResponse` with discount breakdown
- `incrementUsage()` - Track CV/API usage with automatic monthly reset

**Removed**:
- `downgradeUserToFreePlan()` - No longer needed (use `reactivateFreePlan()`)

**Benefits**:
- Cleaner logic - no complex downgrades
- Free plan always exists as fallback
- Only one subscription active at any time
- Automatic monthly usage reset per subscription

---

### 6. **Subscription Lifecycle Service Refactoring**

#### **SubscriptionLifecycleService.java** - Updated

**Removed**: Manual downgrade logic
**Added**: Free plan reactivation flow

**Scheduled Jobs**:
1. **2 AM** - `processExpiredSubscriptions()`
   - Finds expired ACTIVE subscriptions
   - Sets status to EXPIRED
   - **Reactivates free plan automatically**
   - Sends expiration notification

2. **3 AM** - `processOverduePayments()`
   - Finds PENDING subscriptions older than 7 days
   - Sets status to CANCELLED
   - Sends cancellation notification

3. **4 AM** - `processPaymentReminders()`
   - Finds PENDING subscriptions 3-7 days old
   - Sends payment reminder emails

4. **5 AM** - `sendRenewalReminders()`
   - Finds subscriptions expiring in 3 days
   - Sends renewal reminders
   - **Skips free plan** (never expires)

**New Email Notifications**:
- `sendExpirationNotification()` - Subscription expired
- Payment reminder - Complete payment within 7 days
- Cancellation notice - Overdue subscription cancelled

---

### 7. **Repository Updates**

#### **UserSubscriptionRepository.java**
**Updated**:
- All methods now use `SubscriptionStatus` enum (not inner class)
- Added `findByStatusAndStartDateBetween()` for payment reminders
- Added `findByUserIdAndPlanName()` - Find user's free plan

**Removed**:
- Legacy User-based queries (use userId instead)

---

### 8. **Configuration Updates**

#### **application.properties**
**Added**:
```properties
# Paymob Payment Gateway
payment.paymob.base-url=https://accept.paymob.com
payment.paymob.api-key=${PAYMOB_API_KEY:your_key_here}
payment.paymob.integration-id=${PAYMOB_INTEGRATION_ID:your_id_here}
payment.paymob.iframe-id=${PAYMOB_IFRAME_ID:your_iframe_id}
payment.paymob.hmac-secret=${PAYMOB_HMAC_SECRET:}

# AI Service
ai.default.provider=gemini
ai.default.model=gemini-pro
ai.fallback.provider=
```

---

## 📋 Payment & Subscription Flow

### **User Registration Flow**
1. User registers via `/api/auth/register`
2. `AuthServiceImpl` creates user
3. `createDefaultFreeSubscription()` called automatically
4. User gets FREE plan subscription (expires in 100 years = never)
5. User is now `ROLE_USER` with free plan active

### **Subscription Upgrade Flow**
1. User browses plans: `GET /api/subscriptions/plans`
2. User calculates price: `GET /api/subscriptions/calculate-price?planId=2&promoCode=SAVE20`
3. User initiates payment: `POST /api/payments/initiate`
   ```json
   {
     "planId": 2,
     "promoCode": "SAVE20",
     "isAnnual": false
   }
   ```
4. System creates:
   - UserSubscription (status=PENDING)
   - PaymentTransaction (status=PENDING)
   - Links them together
5. System returns payment URL:
   ```json
   {
     "paymentKey": "ZXlKMGVYQWlP...",
     "orderId": "123456",
     "subscriptionId": 42,
     "amount": 199.00,
     "currency": "EGP",
     "paymentUrl": "https://accept.paymob.com/api/acceptance/iframes/12345?payment_token=ZXlKMGVYQWlP...",
     "message": "Payment initiated successfully"
   }
   ```
6. Frontend redirects user to `paymentUrl`
7. User completes payment on Paymob
8. Paymob sends webhook to `/api/payments/webhook`
9. System processes callback:
   - Updates PaymentTransaction (status=COMPLETED)
   - Calls `activateSubscription(paymentKey)`
   - **Deactivates old subscriptions (including free plan)**
   - Activates new subscription
   - Updates user role if plan has dashboard access
10. User now has paid plan active!

### **Subscription Expiration Flow**
1. Scheduled job runs daily at 2 AM
2. Finds subscriptions where `endDate < now` AND `status=ACTIVE`
3. For each expired subscription:
   - Sets status=EXPIRED
   - Calls `reactivateFreePlan(userId)`
   - Finds user's free plan subscription
   - Sets free plan status=ACTIVE
   - Downgrades user role to ROLE_USER
   - Sends expiration email
4. User automatically back on free plan!

### **Payment Reminder Flow**
1. Scheduled job runs daily at 4 AM
2. Finds subscriptions where:
   - status=PENDING
   - startDate between 3-7 days ago
3. Sends payment reminder email to complete payment
4. After 7 days (next day's 3 AM job):
   - Automatically cancels overdue subscription
   - Sends cancellation notification

---

## 🎯 Key Benefits of Refactoring

1. **No Redundant Logic**: Usage tracking only in UserSubscription
2. **Automatic Free Plan**: Always exists as fallback, no manual downgrade
3. **Complete Paymob Integration**: Full payment cycle with no placeholders
4. **Better Architecture**: Enums separated, DTOs for APIs, clean separation
5. **Robust Error Handling**: Try-catch blocks, proper logging, fallback mechanisms
6. **Monthly Usage Reset**: Automatic counter reset on first use each month
7. **Email Notifications**: Users informed of all subscription events
8. **Single Source of Truth**: One active subscription at a time - no confusion

---

## 🚀 How to Use

### **Setup Paymob**
1. Get your Paymob credentials from https://accept.paymob.com
2. Set environment variables:
   ```bash
   export PAYMOB_API_KEY="your_api_key"
   export PAYMOB_INTEGRATION_ID="123456"
   export PAYMOB_IFRAME_ID="654321"
   ```
3. Or update `application.properties` directly

### **Create Free Plan**
Run SQL to create free plan (required for system to work):
```sql
INSERT INTO subscription_plans (name, description, details, cv_limit, api_limit, watermark, dashboard, monthly_price, annual_price, active, is_deleted)
VALUES ('free', 'Free Plan', 'Basic features for getting started', 5, 500, true, false, 0, 0, true, false);
```

### **Test Payment Flow**
```bash
# 1. Register user
POST /api/auth/register
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Password123"
}

# 2. View plans
GET /api/subscriptions/plans

# 3. Calculate price
GET /api/subscriptions/calculate-price?planId=2&isAnnual=false

# 4. Initiate payment
POST /api/payments/initiate
{
  "planId": 2,
  "isAnnual": false
}

# 5. Complete payment on Paymob (use returned paymentUrl)

# 6. Verify activation
GET /api/subscriptions/my-subscription
```

---

## 📊 Database Schema Changes

### **Required Migrations**
```sql
-- Add new enum columns
ALTER TABLE user_subscriptions ADD COLUMN usage_reset_date TIMESTAMP;

-- Remove old User tracking columns
ALTER TABLE users DROP COLUMN IF EXISTS api_usage_month;
ALTER TABLE users DROP COLUMN IF EXISTS api_usage_count;
ALTER TABLE users DROP COLUMN IF EXISTS cv_count;
ALTER TABLE users DROP COLUMN IF EXISTS ui_cv_count;
ALTER TABLE users DROP COLUMN IF EXISTS ui_cv_month;

-- Update PaymentTransaction
ALTER TABLE payment_transactions
  ALTER COLUMN amount TYPE DECIMAL(19,2),
  ADD COLUMN currency VARCHAR(3) DEFAULT 'EGP',
  ADD COLUMN subscription_id BIGINT;
```

---

## ⚠️ Breaking Changes

1. **User Entity**: Removed usage tracking methods
   - **Before**: `user.canUseApi()`, `user.incrementApiUsage()`
   - **After**: Use `SubscriptionService.canUserUseFeature()`, `incrementUsage()`

2. **Repository Methods**: Changed enum type
   - **Before**: `UserSubscription.SubscriptionStatus`
   - **After**: `com.app.civify.entity.enums.SubscriptionStatus`

3. **Service Interface**: Changed return type
   - **Before**: `BigDecimal calculateTotalPrice()`
   - **After**: `PriceCalculationResponse calculateTotalPrice()` (includes breakdown)

4. **Removed Method**: `downgradeUserToFreePlan()`
   - Use `reactivateFreePlan()` instead

---

## 🧪 Next Steps (Remaining Work)

1. **Update Controllers** to use new DTOs
2. **Fix Unit Tests** for new structure
3. **AI Service Enhancement** (multi-model support)
4. **Integration Tests** for complete payment flow
5. **API Documentation** (Swagger/OpenAPI)

---

## 📝 Notes

- This refactoring maintains backward compatibility where possible
- Database migrations are required before deploying
- Ensure environment variables are set for Paymob
- Free plan must exist in database before system can function
- Scheduled jobs require time zone configuration

---

**Refactoring completed by**: Claude AI Assistant
**Date**: 2025-10-18
**Version**: 2.0.0
