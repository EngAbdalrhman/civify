# Civify Architecture Documentation

## Entity Relationship Diagram

### Payment & Subscription System

```mermaid
erDiagram
    User ||--o{ UserSubscription : has
    User ||--o{ PaymentTransaction : makes
    SubscriptionPlan ||--o{ UserSubscription : defines
    PromoCode ||--o{ UserSubscription : applies
    
    User {
        Long id PK
        String email
        String name
        Integer extraTokens "Purchased token wallet"
        Integer cvLimit "Custom override"
    }
    
    SubscriptionPlan {
        Long id PK
        String name
        BigDecimal monthlyPrice
        BigDecimal annualPrice
        Integer cvLimit "null = unlimited"
        Integer tokenLimit "Monthly allowance"
        Boolean isFree
        Boolean isActive
        Boolean renewalAllowed
    }
    
    UserSubscription {
        Long id PK
        Long userId FK
        Long planId FK
        LocalDateTime startDate
        LocalDateTime endDate
        SubscriptionStatus status "PENDING|ACTIVE|CANCELLED|EXPIRED"
        Integer currentCvCount "Usage counter"
        Integer currentTokenUsage "Monthly tokens used"
        Boolean autoRenew
        Integer currentCycleCount
        String paymentTransactionId
    }
    
    PaymentTransaction {
        String id PK
        Long userId FK
        String orderId "Gateway order ID"
        String paymentKey
        PaymentStatus status "PENDING|COMPLETED|FAILED|REFUNDED"
        BigDecimal amount
        String currency
        String gateway "paymob|fawaterak"
        Long subscriptionId FK
    }
    
    PromoCode {
        Long id PK
        String code
        DiscountType type "PERCENTAGE|FIXED"
        BigDecimal discountValue
        Integer maxUses
        LocalDateTime expiresAt
    }
```

### Subscription Lifecycle

```mermaid
flowchart TD
    A[User Selects Plan] --> B[Payment Initiated]
    B --> C[PaymentTransaction Created]
    C --> D{Payment Success?}
    D -->|Yes| E[UserSubscription ACTIVE]
    D -->|No| F[Retry/Cancel]
    E --> G[Usage Tracked]
    G --> H{End Date Reached?}
    H -->|Yes & autoRenew| I[Renewal Payment]
    I --> D
    H -->|Yes & !autoRenew| J[Status → EXPIRED]
    H -->|No| G
    E --> K[User Cancels]
    K --> L[autoRenew = false]
    L --> G
```

---

## AI Service Architecture

```mermaid
flowchart TD
    subgraph Frontend
        A[Upload CV File] --> B[/api/v1/cvs/upload]
        C[Enter Job Description] --> D[/api/v1/cvs/tailor]
    end
    
    subgraph Backend["Backend (CvUseCaseImpl)"]
        B --> E[CvParsingService]
        D --> F[CvEnhancementService]
    end
    
    subgraph AI["AI Service Layer"]
        E --> G[AiServiceImpl]
        F --> G
        G --> H[GeminiAiClient]
        H --> I[Google Gemini API]
    end
    
    subgraph Logging
        E --> J[CvOperationLogService]
        F --> J
    end
    
    I --> K[Return JSON Response]
    K --> L[Parse to ResumeData]
```

### AI Provider Pattern

```mermaid
classDiagram
    class AiService {
        <<interface>>
        +parseCv(file) String
        +tailorCv(cvId, jobDescription) String
        +callModel(prompt, provider, model) String
    }
    
    class AiServiceImpl {
        -providers: Map~String,AiProvider~
        +registerProvider(name, provider)
        +callModelWithFallback(prompt, provider, model)
    }
    
    class AiProvider {
        <<interface>>
        +callModel(prompt, model) String
        +getProviderName() String
        +isAvailable() boolean
    }
    
    class GeminiProvider {
        -client: GeminiAiClient
        +callModel(prompt, model) String
    }
    
    AiService <|.. AiServiceImpl
    AiServiceImpl --> AiProvider
    AiProvider <|.. GeminiProvider
    GeminiProvider --> GeminiAiClient
```

---

## Entity Usage Summary

| Entity | Table | Status | Purpose |
|--------|-------|--------|---------|
| **Resume** | `resumes` | ✅ ACTIVE | User-editable resume data from builder |
| **Cv** | `cvs` | ⚠️ UNUSED | Designed for AI artifacts, not populated |
| **JobApplication** | `job_applications` | ✅ ACTIVE | Tracks job applications with tailored CV |

### Cv vs Resume

- **Resume**: User's main resume data, edited via UI builder, stored as JSON
- **Cv**: Legacy/unused entity for storing raw AI parsing results

### JobApplication Flow (Enhancement Opportunity)

Current: User tailors CV → Downloads PDF → Manually tracks

**Suggested Enhancement:**
1. After tailoring, prompt "Save to Job Tracker?"
2. Create `JobApplication` with:
   - Job description user entered
   - Tailored CV reference
   - Company/title from Step 3
3. Track application status (Applied, Interview, Offer)
4. Dashboard showing all applications

---

## Environment Configuration

### Payment Gateways
- `PAYMOB_API_KEY` - Paymob authentication
- `PAYMOB_INTEGRATION_ID` - Card integration
- `PAYMOB_IFRAME_ID` - Checkout iframe
- `PAYMOB_HMAC_SECRET` - Callback verification

### AI Service
- `GEMINI_API_KEY` - Google Gemini API key
- `gemini.base.url` - API base URL
