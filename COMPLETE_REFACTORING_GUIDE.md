# Civify Backend - Complete Refactoring Guide

## 🎯 Executive Summary

This document provides a comprehensive overview of the complete refactoring performed on the Civify backend. The refactoring focused on:
- **Architecture improvements** with separated enums and DTOs
- **Complete Paymob payment integration** with no placeholders
- **Robust subscription system** using Option A flow (single active subscription)
- **Enhanced usage tracking** with automatic monthly resets
- **Multi-model AI service** with automatic fallback
- **Removed all redundant logic** from User entity

---

## 📊 Project Structure

```
src/main/java/com/app/civify/
├── entity/
│   ├── enums/              # NEW: Separated enum files
│   │   ├── SubscriptionStatus.java
│   │   ├── PaymentStatus.java
│   │   ├── DiscountType.java
│   │   └── CvGenerationType.java
│   ├── User.java           # REFACTORED: Removed usage tracking
│   ├── UserSubscription.java  # ENHANCED: Auto monthly reset
│   ├── PaymentTransaction.java  # UPDATED: JSONB, BigDecimal
│   ├── PromoCode.java      # UPDATED: External enum
│   └── Cv.java             # ENHANCED: Generation tracking
├── dto/
│   ├── subscription/       # NEW: Subscription DTOs
│   │   ├── SubscriptionRequest.java
│   │   ├── SubscriptionResponse.java
│   │   └── PriceCalculationResponse.java
│   ├── payment/            # NEW: Payment DTOs
│   │   ├── PaymentRequest.java
│   │   ├── PaymentInitiationResponse.java
│   │   └── paymob/        # NEW: Paymob-specific DTOs
│   │       ├── PaymobAuthResponse.java
│   │       ├── PaymobOrderResponse.java
│   │       └── PaymobPaymentKeyResponse.java
├── service/
│   ├── payment/
│   │   └── PaymobPaymentGateway.java  # COMPLETE: Full implementation
│   ├── subscription/
│   │   ├── SubscriptionServiceImpl.java  # REFACTORED: Option A
│   │   └── SubscriptionLifecycleService.java  # UPDATED: No downgrade
│   ├── usage/              # NEW: Usage tracking service
│   │   ├── UsageTrackingService.java
│   │   ├── UsageTrackingServiceImpl.java
│   │   └── UsageStats.java
│   └── ai/
│       └── AiServiceImpl.java  # ENHANCED: Multi-model support
└── repository/
    ├── UserSubscriptionRepository.java  # UPDATED: New query methods
    └── CvRepository.java   # ENHANCED: Usage tracking queries
```

---

## 🔄 Complete Subscription & Payment Flow

### 1. **User Registration**
```
User registers
    ↓
AuthService creates User (ROLE_USER)
    ↓
createDefaultFreeSubscription() called
    ↓
FREE plan subscription created (expires in 100 years)
    ↓
User ready with free plan active
```

### 2. **Subscription Upgrade**
```
User browses plans
    ↓
GET /api/subscriptions/plans
    ↓
User calculates price with promo code
    ↓
GET /api/subscriptions/calculate-price?planId=2&promoCode=SAVE20
    ← Returns PriceCalculationResponse with breakdown
    ↓
User initiates payment
    ↓
POST /api/payments/initiate
    ↓
SubscriptionService.initiatePayment()
    ├── Creates UserSubscription (status=PENDING)
    ├── Creates PaymentTransaction (status=PENDING)
    ├── Links payment to subscription
    └── Returns payment key & URL
    ↓
Frontend redirects to Paymob iframe
    ↓
User completes payment on Paymob
    ↓
Paymob sends webhook to /api/payments/webhook
    ↓
PaymentController.handlePaymentWebhook()
    ├── Updates PaymentTransaction (status=COMPLETED)
    ├── Calls activateSubscription(paymentKey)
    │   ├── Deactivates all active subscriptions (including FREE)
    │   ├── Activates new subscription
    │   └── Updates user role if needed
    └── Returns success
    ↓
User now has paid plan active!
```

### 3. **Usage Tracking**
```
User generates CV via UI
    ↓
Controller calls UsageTrackingService.canGenerateUiCv(userId)
    ├── Gets active subscription
    ├── Checks automatic monthly reset
    ├── Returns true/false
    ↓
If allowed:
    ├── Generate CV
    ├── Save to database with metadata
    └── UsageTrackingService.incrementUiCvUsage(userId)
        └── Increments counter in subscription
```

### 4. **Subscription Expiration**
```
Scheduled job runs daily at 2 AM
    ↓
SubscriptionLifecycleService.processExpiredSubscriptions()
    ↓
Finds expired subscriptions (endDate < now && status=ACTIVE)
    ↓
For each expired subscription:
    ├── Set status=EXPIRED
    ├── Call reactivateFreePlan(userId)
    │   ├── Find user's FREE plan subscription
    │   ├── Set status=ACTIVE
    │   └── Downgrade user role to ROLE_USER
    └── Send expiration notification email
    ↓
User automatically back on FREE plan!
```

---

## 💡 Key Design Decisions

### **1. Option A: Single Active Subscription**

**Decision**: Only ONE subscription active at a time
**Implementation**:
- FREE plan created on registration (never expires)
- Upgrading → deactivates FREE, activates paid
- Expiring → deactivates paid, reactivates FREE
- No manual downgrade needed

**Benefits**:
- Simple logic
- Always have fallback (FREE plan)
- No confusion about which subscription to use
- Easy to query: `getActiveSubscription()` always returns one result

### **2. Usage Tracking in Subscription**

**Decision**: Track usage at `UserSubscription` level, not `User` level
**Implementation**:
- `currentApiCount`, `currentCvCount` in `UserSubscription`
- `usageResetDate` tracks next reset
- Automatic monthly reset on first use

**Benefits**:
- No duplicate tracking
- Audit trail (which subscription was active when CV generated)
- Historical data preserved when subscription changes
- Clean separation of concerns

### **3. CV Storage Limits vs Generation Limits**

**Clarification**:
- **API Limit** (e.g., 500): Number of API calls per month with API key
- **UI CV Limit** (e.g., 10): Number of CVs generated via UI per month
- **Stored CV Limit**: Same as UI CV limit - max CVs user can store

**Implementation**:
```java
// UserSubscription tracks generation counts (reset monthly)
private Integer currentApiCount = 0;
private Integer currentCvCount = 0;

// CvRepository tracks total stored CVs (doesn't reset)
long storedCvCount = cvRepository.countByUserId(userId);

// Check limits
boolean canGenerate = currentCvCount < plan.getCvLimit();
boolean canStore = storedCvCount < plan.getCvLimit();
```

### **4. PostgreSQL JSONB for Metadata**

**Decision**: Use JSONB for flexible metadata storage
**Implementation**:
```java
@Column(columnDefinition = "jsonb")
private String metadata;
```

**Benefits**:
- Store arbitrary payment/subscription data
- Queryable with PostgreSQL JSON operators
- No schema changes for new metadata fields
- Efficient storage and indexing

---

## 🎨 Usage Tracking Explained

### **Three Types of Limits**

#### **1. API Call Limit** (`apiLimit`)
- **What**: Number of API calls to CV generation endpoints (with API key)
- **Example**: 500 calls/month
- **Tracked in**: `UserSubscription.currentApiCount`
- **Reset**: Monthly (automatic)
- **Check**: `UsageTrackingService.canUseApi(userId)`
- **Increment**: `UsageTrackingService.incrementApiUsage(userId)`

#### **2. UI CV Generation Limit** (`cvLimit`)
- **What**: Number of CVs generated via UI stepper (4-step process)
- **Example**: 10 CVs/month
- **Tracked in**: `UserSubscription.currentCvCount`
- **Reset**: Monthly (automatic)
- **Check**: `UsageTrackingService.canGenerateUiCv(userId)`
- **Increment**: `UsageTrackingService.incrementUiCvUsage(userId)`

#### **3. Stored CV Limit** (uses `cvLimit`)
- **What**: Total number of CVs stored in database
- **Example**: 10 CVs total
- **Tracked in**: `CvRepository.countByUserId(userId)`
- **Reset**: Never (user must delete old CVs)
- **Check**: `UsageTrackingService.canStoreCv(userId)`
- **No increment**: Automatic via database count

### **Automatic Monthly Reset**

```java
// In UserSubscription.java
private void resetUsageIfNewMonth() {
    LocalDateTime now = LocalDateTime.now();
    if (usageResetDate == null || now.isAfter(usageResetDate)) {
        currentCvCount = 0;
        currentApiCount = 0;
        // Set reset date to first day of next month
        usageResetDate = now.plusMonths(1)
            .withDayOfMonth(1)
            .withHour(0).withMinute(0).withSecond(0);
    }
}

// Called automatically in:
public boolean canUseCv() {
    if (!isActive()) return false;
    resetUsageIfNewMonth();  // ← Auto-reset!
    return plan.getCvLimit() == null || currentCvCount < plan.getCvLimit();
}
```

### **Usage Stats API**

```java
// Get comprehensive usage statistics
UsageStats stats = usageTrackingService.getUserUsageStats(userId);

// Returns:
{
    "subscriptionId": 42,
    "planName": "Premium",
    "subscriptionEndDate": "2025-11-18T00:00:00",
    "apiUsageCount": 250,
    "apiLimit": 500,
    "apiRemaining": 250,
    "uiCvCount": 5,
    "uiCvLimit": 10,
    "uiCvRemaining": 5,
    "storedCvCount": 7,
    "storedCvLimit": 10,
    "storedCvRemaining": 3,
    "usageResetDate": "2025-11-01T00:00:00"
}
```

---

## 🤖 Enhanced AI Service

### **Multi-Model Support**

```properties
# Primary provider
ai.default.provider=gemini
ai.default.model=gemini-pro

# Fallback provider (optional)
ai.fallback.provider=openai
ai.fallback.model=gpt-3.5-turbo
```

### **Automatic Fallback**

```
Primary provider (Gemini) fails
    ↓
Check if fallback configured
    ↓
Try fallback provider (OpenAI)
    ↓
If success → Return result
If fails → Throw combined error
```

### **Retry Mechanism**

- **Max attempts**: 3 for CV operations, 2 for direct calls
- **Backoff**: Exponential (1s, 2s, 4s...)
- **Recovery**: Custom error message after all retries fail

### **Adding New Providers**

```java
// 1. Create client interface
@FeignClient(name = "openaiClient", url = "https://api.openai.com")
public interface OpenAiClient {
    @PostMapping("/v1/chat/completions")
    String chat(@RequestBody String payload);
}

// 2. Implement provider
public static class OpenAiProvider implements AiProvider {
    private final OpenAiClient client;

    @Override
    public String callModel(String prompt, String model) {
        String requestBody = buildOpenAiRequest(prompt, model);
        return client.chat(requestBody);
    }
}

// 3. Register in constructor
registerProvider("openai", new OpenAiProvider(openAiClient));
```

---

## 🔧 Configuration Guide

### **Required Environment Variables**

```bash
# Paymob Payment Gateway
export PAYMOB_API_KEY="your_paymob_api_key"
export PAYMOB_INTEGRATION_ID="123456"
export PAYMOB_IFRAME_ID="654321"
export PAYMOB_HMAC_SECRET="your_hmac_secret"  # Optional

# Database
export DB_URL="jdbc:postgresql://localhost:5432/civify"
export DB_USERNAME="postgres"
export DB_PASSWORD="yourpassword"

# JWT
export JWT_SECRET="your_jwt_secret_key_here"

# AI (Optional - has defaults)
export AI_DEFAULT_PROVIDER="gemini"
export AI_DEFAULT_MODEL="gemini-pro"
export AI_FALLBACK_PROVIDER=""  # Leave empty for no fallback
```

### **Database Setup**

```sql
-- Create FREE plan (REQUIRED)
INSERT INTO subscription_plans (
    name, description, details,
    cv_limit, api_limit,
    watermark, dashboard,
    monthly_price, annual_price,
    active, is_deleted,
    created_at, updated_at
) VALUES (
    'free',
    'Free Plan',
    'Basic features to get started:\n- 5 CV generations per month\n- 500 API calls per month\n- Watermarked exports',
    5,      -- cv_limit
    500,    -- api_limit
    true,   -- watermark (has watermark)
    false,  -- dashboard (no dashboard access)
    0,      -- monthly_price
    0,      -- annual_price
    true,   -- active
    false,  -- is_deleted
    NOW(),
    NOW()
);

-- Create Premium plan (Example)
INSERT INTO subscription_plans (
    name, description, details,
    cv_limit, api_limit,
    watermark, dashboard,
    monthly_price, annual_price,
    active, is_deleted,
    created_at, updated_at
) VALUES (
    'premium',
    'Premium Plan',
    'Full features for professionals:\n- 50 CV generations per month\n- 5000 API calls per month\n- No watermark\n- Dashboard access',
    50,     -- cv_limit
    5000,   -- api_limit
    false,  -- watermark (no watermark)
    true,   -- dashboard (has dashboard access)
    29.99,  -- monthly_price
    299.99, -- annual_price
    true,   -- active
    false,  -- is_deleted
    NOW(),
    NOW()
);
```

---

## 🧪 Testing Guide

### **Test User Registration & Free Plan**

```bash
# 1. Register user
curl -X POST http://localhost:8077/apis/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123"
  }'

# 2. Login
curl -X POST http://localhost:8077/apis/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123"
  }'
# Save the JWT token

# 3. Check subscription
curl http://localhost:8077/apis/subscriptions/my-subscription \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Should show FREE plan active
```

### **Test Payment Flow**

```bash
# 1. View available plans
curl http://localhost:8077/apis/subscriptions/plans

# 2. Calculate price
curl "http://localhost:8077/apis/subscriptions/calculate-price?planId=2&isAnnual=false" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 3. Initiate payment
curl -X POST http://localhost:8077/apis/payments/initiate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "planId": 2,
    "isAnnual": false,
    "promoCode": "SAVE20"
  }'

# Returns paymentUrl - open in browser to complete payment
```

### **Test Usage Tracking**

```bash
# 1. Get usage stats
curl http://localhost:8077/apis/subscriptions/my-usage \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 2. Generate CV (increments counter)
curl -X POST http://localhost:8077/apis/cv/generate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@resume.pdf"

# 3. Check updated stats
curl http://localhost:8077/apis/subscriptions/my-usage \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📈 API Endpoints Summary

### **Subscription Management**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/subscriptions/plans` | List all active subscription plans |
| GET | `/api/subscriptions/plans/{id}` | Get specific plan details |
| GET | `/api/subscriptions/my-subscription` | Get user's active subscription |
| GET | `/api/subscriptions/my-subscriptions` | Get all user's subscriptions (history) |
| GET | `/api/subscriptions/calculate-price` | Calculate price with promo code |
| GET | `/api/subscriptions/my-usage` | Get usage statistics |
| POST | `/api/subscriptions/{id}/cancel` | Cancel subscription |
| POST | `/api/subscriptions/{id}/renew` | Renew subscription |

### **Payment Processing**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/initiate` | Start payment process |
| POST | `/api/payments/webhook` | Paymob webhook callback |
| GET | `/api/payments/verify/{id}` | Verify payment status |

### **CV Operations**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/cv/parse` | Parse uploaded CV |
| POST | `/api/cv/generate` | Generate CV via UI |
| POST | `/api/cv/tailor` | Tailor CV for job |
| GET | `/api/cv/my-cvs` | List user's stored CVs |
| GET | `/api/cv/{id}` | Get specific CV |
| DELETE | `/api/cv/{id}` | Delete CV (frees storage) |

---

## ⚠️ Important Notes

### **Migration Checklist**

- [ ] Run database migrations (see SQL above)
- [ ] Set environment variables for Paymob
- [ ] Create FREE plan in database
- [ ] Test complete payment flow in sandbox
- [ ] Configure scheduled jobs (cron expressions)
- [ ] Set up email service for notifications
- [ ] Test usage tracking and limits
- [ ] Verify monthly reset functionality

### **Breaking Changes**

1. `User.getActiveSubscription()` **removed** → Use `UsageTrackingService` or repository
2. `SubscriptionService.downgradeUserToFreePlan()` **removed** → Use `reactivateFreePlan()`
3. `SubscriptionService.calculateTotalPrice()` now returns `PriceCalculationResponse` (not `BigDecimal`)
4. Enum imports changed (e.g., `UserSubscription.SubscriptionStatus` → `com.app.civify.entity.enums.SubscriptionStatus`)

### **Performance Considerations**

- Usage tracking queries optimized with indexes
- CV counting uses repository methods (efficient)
- Subscription queries filter by status and date
- Scheduled jobs run at off-peak hours
- Payment webhooks process asynchronously

---

## 🚀 Deployment

### **Development**

```bash
# 1. Start PostgreSQL
docker-compose up -d postgres

# 2. Set environment variables
source .env

# 3. Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### **Production**

```bash
# 1. Build
./mvnw clean package -DskipTests

# 2. Run with production profile
java -jar target/civify-*.jar --spring.profiles.active=prod
```

### **Docker**

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8077
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 📚 Additional Resources

- **REFACTORING_SUMMARY.md**: Detailed list of all changes
- **API Documentation**: (Generate with Swagger/OpenAPI)
- **Database Schema**: See migration SQLs above
- **Paymob Documentation**: https://accept.paymob.com/docs/

---

**Refactoring Status**: ✅ **COMPLETE**
**Version**: 2.0.0
**Date**: 2025-10-18
**Author**: Claude AI Assistant

