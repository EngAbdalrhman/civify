# Security & Filter Service Refactoring Plan

## 🔐 Security Refactoring

### Issues Identified

1. **Redundant Token Generation**: Multiple overloaded methods in `JwtUtil` doing the same thing
2. **Inconsistent Token Handling**: `TokenService` uses Redis for refresh tokens but `JwtUtil` generates JWT refresh tokens
3. **Missing Email Verification**: Registration returns tokens immediately without email verification
4. **Incomplete 2FA Flow**: 2FA implementation is basic and doesn't integrate properly
5. **OAuth2 Inconsistency**: OAuth users skip verification but manual users don't

### Proposed Solution

#### **Clean Token Architecture**

```
JwtUtil (Simplified)
├── generateToken(User user) → Returns JWT with all user claims
└── validateAndExtract(String token) → Returns UserClaims

TokenService (Simplified)
├── createAuthTokens(User user) → Returns AuthTokenPair (access + refresh)
├── refreshAccessToken(String refreshToken) → Returns new access token
└── revokeRefreshToken(String refreshToken) → Revokes refresh token

AuthService
├── register(AuthRequest) → Returns message "Check email for verification"
├── verifyEmail(String token) → Activates account
├── login(AuthRequest) → Returns tokens OR 2FA challenge
├── verify2FA(String username, String otp) → Returns tokens
└── loginWithOAuth2(OAuth2User) → Returns tokens (email pre-verified)
```

#### **Auth Flow**

**Flow 1: Email/Password Registration**
```
POST /auth/register
├── Create user (isEmailVerified=false)
├── Generate email verification token
├── Send verification email
└── Return: { "message": "Check your email to verify account" }

User clicks verification link
↓
GET /auth/verify-email?token=xxx
├── Validate token
├── Set user.isEmailVerified = true
├── Create default FREE subscription
└── Return: { "message": "Email verified! You can now login" }

POST /auth/login
├── Validate credentials
├── Check if email verified (throw error if not)
├── Check if 2FA enabled
│   ├── If YES: Generate OTP → Send email → Return: { "requires2FA": true }
│   └── If NO: Generate tokens → Return: { "accessToken", "refreshToken" }

POST /auth/verify-2fa (if 2FA enabled)
├── Validate OTP
├── Generate tokens
└── Return: { "accessToken", "refreshToken" }
```

**Flow 2: OAuth2 (Google) Registration/Login**
```
GET /oauth2/authorization/google
↓
Google redirects back with auth code
↓
OAuth2SuccessHandler
├── Extract user info from Google
├── Check if user exists
│   ├── If NO: Create user (isEmailVerified=true, oauthProvider="google")
│   │         Create default FREE subscription
│   └── If YES: Update last login
├── Generate tokens
└── Redirect to frontend with tokens
```

---

## 🔍 Generic Filter Service Implementation

### Current State
- Filter models exist (`FilterCriteria`, `FilterGroup`, etc.)
- `CommonRepo` interface exists
- **Missing**: Actual filter implementation with JPA Specifications

### Complete Implementation

#### **1. Generic Filter Service**

```java
@Service
public class GenericFilterService {

    public <T> Page<T> findWithFilters(
        InputDataModel filterModel,
        JpaSpecificationExecutor<T> repository,
        Class<T> entityClass
    ) {
        Specification<T> spec = buildSpecification(filterModel);
        Pageable pageable = buildPageable(filterModel);
        return repository.findAll(spec, pageable);
    }

    private <T> Specification<T> buildSpecification(InputDataModel model) {
        // Build JPA Specification from filters
        Specification<T> spec = Specification.where(null);

        // Add default filters (AND operation)
        for (FilterCriteria filter : model.getFilters()) {
            spec = spec.and(buildPredicate(filter));
        }

        // Add filter groups
        for (FilterGroup group : model.getFilterGroups()) {
            Specification<T> groupSpec = buildGroupSpec(group);
            if (model.getOperation() == LogicalOperation.AND) {
                spec = spec.and(groupSpec);
            } else {
                spec = spec.or(groupSpec);
            }
        }

        return spec;
    }

    private <T> Specification<T> buildPredicate(FilterCriteria criteria) {
        return (root, query, cb) -> {
            Path<?> path = getNestedPath(root, criteria.getFieldName());

            switch (criteria.getOperation()) {
                case EQUALS:
                    return cb.equal(path, criteria.getValue());
                case NOT_EQUALS:
                    return cb.notEqual(path, criteria.getValue());
                case GREATER_THAN:
                    return cb.greaterThan((Path<Comparable>) path, (Comparable) criteria.getValue());
                case LESS_THAN:
                    return cb.lessThan((Path<Comparable>) path, (Comparable) criteria.getValue());
                case LIKE:
                    return cb.like(cb.lower((Path<String>) path), "%" + criteria.getValue().toString().toLowerCase() + "%");
                case IN:
                    return path.in((Collection<?>) criteria.getValue());
                case IS_NULL:
                    return cb.isNull(path);
                case IS_NOT_NULL:
                    return cb.isNotNull(path);
                case BETWEEN:
                    return cb.between((Path<Comparable>) path,
                        (Comparable) criteria.getValue(),
                        (Comparable) criteria.getValue2());
                default:
                    throw new UnsupportedOperationException("Operation not supported: " + criteria.getOperation());
            }
        };
    }

    private Path<?> getNestedPath(Root<?> root, String fieldName) {
        String[] fields = fieldName.split("\\.");
        Path<?> path = root;
        for (String field : fields) {
            path = path.get(field);
        }
        return path;
    }

    private Pageable buildPageable(InputDataModel model) {
        Sort sort = Sort.unsorted();
        for (SortCriteria sortCriteria : model.getSorts()) {
            Sort.Direction direction = sortCriteria.getDirection() == SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
            sort = sort.and(Sort.by(direction, sortCriteria.getFieldName()));
        }
        return PageRequest.of(model.getPage(), model.getSize(), sort);
    }
}
```

#### **2. Usage Example - ApiUsageLog Filtering**

```java
@RestController
@RequestMapping("/api/logs")
public class ApiUsageLogController {

    @Autowired
    private ApiUsageLogRepository repository;

    @Autowired
    private GenericFilterService filterService;

    @PostMapping("/filter")
    public PagedResponse<ApiUsageLog> filterLogs(@RequestBody InputDataModel filterModel) {
        Page<ApiUsageLog> page = filterService.findWithFilters(
            filterModel,
            repository,
            ApiUsageLog.class
        );
        return PagedResponse.of(page);
    }
}

// Example Request Body:
{
    "filters": [
        { "fieldName": "userId", "operation": "EQUALS", "value": 123 },
        { "fieldName": "createdAt", "operation": "BETWEEN", "value": "2025-01-01", "value2": "2025-01-31" }
    ],
    "sorts": [
        { "fieldName": "createdAt", "direction": "DESC" }
    ],
    "page": 0,
    "size": 20
}
```

#### **3. Repository Requirements**

```java
// All repositories that need filtering should extend CommonRepo
@Repository
public interface ApiUsageLogRepository extends CommonRepo<ApiUsageLog, Long> {
    // Custom queries if needed
}

@Repository
public interface UserSubscriptionRepository extends CommonRepo<UserSubscription, Long> {
    // Existing custom queries + filtering capability
}
```

---

## 📋 Implementation Checklist

### Phase 1: Security Refactoring
- [ ] Simplify `JwtUtil` - single `generateToken(User)` method
- [ ] Clean up `TokenService` - only handle refresh token lifecycle
- [ ] Add email verification token generation
- [ ] Implement `EmailVerificationService`
- [ ] Refactor `AuthServiceImpl`:
  - [ ] Registration returns message (not tokens)
  - [ ] Add `verifyEmail(String token)` method
  - [ ] Login checks email verification
  - [ ] Proper 2FA flow
- [ ] Update `OAuth2SuccessHandler`:
  - [ ] Check if user exists
  - [ ] Create user with `isEmailVerified=true`
  - [ ] Create default FREE subscription
  - [ ] Generate tokens
- [ ] Add new endpoints:
  - [ ] `POST /auth/register` - Returns message
  - [ ] `GET /auth/verify-email?token=xxx` - Verifies email
  - [ ] `POST /auth/resend-verification` - Resends email
  - [ ] `POST /auth/verify-2fa` - Verifies OTP

### Phase 2: Generic Filter Service
- [ ] Create `GenericFilterService`
- [ ] Implement `buildSpecification()`
- [ ] Implement `buildPredicate()` for all operations
- [ ] Support nested field paths (e.g., "user.email")
- [ ] Update `CommonRepo` if needed
- [ ] Create example controller for `ApiUsageLog`
- [ ] Add Swagger documentation for filter API

### Phase 3: Testing
- [ ] Unit tests for `JwtUtil`
- [ ] Unit tests for `TokenService`
- [ ] Unit tests for `AuthService`
- [ ] Integration tests for auth flows
- [ ] Unit tests for `GenericFilterService`
- [ ] Integration tests for filtering

### Phase 4: Cleanup
- [ ] Delete `files/DataLazyLoad` directory
- [ ] Remove unused imports
- [ ] Update API documentation
- [ ] Update REFACTORING_SUMMARY.md

---

## 🔧 Configuration Updates

### application.properties

```properties
# JWT Configuration
jwt.secret=${JWT_SECRET:your_secret_key_here}
jwt.access-token-expiration=604800000  # 7 days in milliseconds
jwt.refresh-token-expiration=2592000000  # 30 days in milliseconds

# Email Verification
email.verification.token-expiration=86400000  # 24 hours
email.verification.link=http://localhost:8077/apis/auth/verify-email

# 2FA Configuration
2fa.otp.expiration=300000  # 5 minutes
2fa.otp.length=6

# OAuth2 Configuration
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=profile,email
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google
```

---

## 📚 Example API Calls

### Registration & Verification

```bash
# 1. Register
curl -X POST http://localhost:8077/apis/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123"
  }'

# Response:
{
  "message": "Registration successful. Please check your email to verify your account."
}

# 2. User clicks verification link in email
GET http://localhost:8077/apis/auth/verify-email?token=abc123...

# Response:
{
  "message": "Email verified successfully! You can now login.",
  "verified": true
}

# 3. Login
curl -X POST http://localhost:8077/apis/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123"
  }'

# Response (if no 2FA):
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "requires2FA": false
}

# Response (if 2FA enabled):
{
  "requires2FA": true,
  "message": "OTP sent to your email"
}

# 4. Verify 2FA (if enabled)
curl -X POST http://localhost:8077/apis/auth/verify-2fa \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "otp": "123456"
  }'

# Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "requires2FA": false
}
```

### OAuth2 Login

```bash
# User clicks "Login with Google" button
GET http://localhost:8077/oauth2/authorization/google

# Google handles authentication
# Redirects back with tokens

# Frontend receives:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Filtering Example

```bash
# Filter API usage logs
curl -X POST http://localhost:8077/apis/logs/filter \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": [
      {
        "fieldName": "userId",
        "operation": "EQUALS",
        "value": 123
      },
      {
        "fieldName": "endpoint",
        "operation": "LIKE",
        "value": "/api/cv"
      },
      {
        "fieldName": "createdAt",
        "operation": "BETWEEN",
        "value": "2025-01-01T00:00:00",
        "value2": "2025-01-31T23:59:59"
      }
    ],
    "filterGroups": [
      {
        "filters": [
          { "fieldName": "status", "operation": "EQUALS", "value": 200 },
          { "fieldName": "status", "operation": "EQUALS", "value": 201 }
        ],
        "operation": "OR"
      }
    ],
    "operation": "AND",
    "sorts": [
      { "fieldName": "createdAt", "direction": "DESC" }
    ],
    "page": 0,
    "size": 20
  }'

# Response:
{
  "content": [...],  // Array of ApiUsageLog objects
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false,
  "hasNext": true,
  "hasPrevious": false
}
```

---

## 🚨 Breaking Changes

1. **Registration no longer returns tokens** - Returns message to check email
2. **Login requires email verification** - Will throw error if not verified
3. **OAuth2 users auto-verified** - `isEmailVerified=true` by default
4. **Token generation simplified** - Only one method in `JwtUtil`
5. **Refresh token is UUID** - Stored in Redis, not JWT

---

## 📝 Next Steps

1. Review this plan
2. Approve the security flow changes
3. I'll implement all the code changes
4. Update tests
5. Delete DataLazyLoad reference folder
6. Final testing and documentation

**Ready to proceed with implementation?** Let me know if you want any adjustments to the flows!
