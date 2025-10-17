# Civify API Reference

## Base URL
```
Development: http://localhost:8080/api/v1
Production: https://your-domain.com/api/v1
```

---

## Authentication Endpoints

### 1. Register
**POST** `/auth/register`

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "requires2FA": false
}
```

---

### 2. Login
**POST** `/auth/login`

**Request Body:**
```json
{
  "username": "john_doe",
  "password": "SecurePassword123!"
}
```

**Response (without 2FA):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "requires2FA": false
}
```

**Response (with 2FA):**
```json
{
  "accessToken": null,
  "refreshToken": null,
  "requires2FA": true
}
```

---

### 3. Verify OTP (2FA)
**POST** `/auth/verify-otp`

**Request Body:**
```json
{
  "username": "john_doe",
  "code": "123456"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "requires2FA": false
}
```

---

### 4. Refresh Token
**POST** `/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "requires2FA": false
}
```

---

### 5. Logout
**POST** `/auth/logout`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "message": "Logged out successfully"
}
```

---

### 6. Enable 2FA
**POST** `/auth/enable-2fa`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "username": "john_doe"
}
```

**Response:**
```json
{
  "message": "2FA enabled successfully"
}
```

---

### 7. OAuth2 Login (Google)
**GET** `/oauth2/authorization/google`

**Description:** Redirects to Google OAuth2 consent screen. After successful authentication, redirects back to the application with tokens.

---

## Subscription Endpoints

### 1. Get All Plans
**GET** `/subscriptions/plans`

**Response:**
```json
[
  {
    "id": 1,
    "name": "Free",
    "description": "Basic features for individuals",
    "details": "5 CV generations, 100 API calls",
    "cvLimit": 5,
    "apiLimit": 100,
    "watermark": true,
    "dashboard": false,
    "monthlyPrice": 0.00,
    "annualPrice": 0.00,
    "active": true
  },
  {
    "id": 2,
    "name": "Pro",
    "description": "Advanced features for professionals",
    "details": "50 CV generations, 1000 API calls, no watermark, dashboard access",
    "cvLimit": 50,
    "apiLimit": 1000,
    "watermark": false,
    "dashboard": true,
    "monthlyPrice": 19.99,
    "annualPrice": 199.99,
    "active": true
  }
]
```

---

### 2. Get Plan by ID
**GET** `/subscriptions/plans/{planId}`

**Response:**
```json
{
  "id": 2,
  "name": "Pro",
  "description": "Advanced features for professionals",
  "details": "50 CV generations, 1000 API calls, no watermark, dashboard access",
  "cvLimit": 50,
  "apiLimit": 1000,
  "watermark": false,
  "dashboard": true,
  "monthlyPrice": 19.99,
  "annualPrice": 199.99,
  "active": true
}
```

---

### 3. Calculate Price
**POST** `/subscriptions/calculate-price`

**Query Parameters:**
- `planId` (required): Plan ID
- `isAnnual` (optional, default: false): Annual subscription
- `promoCode` (optional): Promo code

**Example:**
```
POST /subscriptions/calculate-price?planId=2&isAnnual=true&promoCode=SAVE20
```

**Response:**
```json
{
  "planId": 2,
  "isAnnual": true,
  "promoCode": "SAVE20",
  "totalPrice": 159.99
}
```

---

### 4. Get My Active Subscription
**GET** `/subscriptions/my-subscription`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "id": 123,
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com"
  },
  "plan": {
    "id": 2,
    "name": "Pro",
    "cvLimit": 50,
    "apiLimit": 1000
  },
  "startDate": "2025-01-01T00:00:00",
  "endDate": "2026-01-01T00:00:00",
  "status": "ACTIVE",
  "currentCvCount": 15,
  "currentApiCount": 250
}
```

---

### 5. Get All My Subscriptions
**GET** `/subscriptions/my-subscriptions`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
[
  {
    "id": 123,
    "plan": {
      "id": 2,
      "name": "Pro"
    },
    "startDate": "2025-01-01T00:00:00",
    "endDate": "2026-01-01T00:00:00",
    "status": "ACTIVE"
  },
  {
    "id": 122,
    "plan": {
      "id": 1,
      "name": "Free"
    },
    "startDate": "2024-01-01T00:00:00",
    "endDate": "2024-12-31T23:59:59",
    "status": "EXPIRED"
  }
]
```

---

### 6. Cancel Subscription
**POST** `/subscriptions/cancel/{subscriptionId}`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "message": "Subscription cancelled successfully"
}
```

---

### 7. Renew Subscription
**POST** `/subscriptions/renew/{subscriptionId}`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
- `isAnnual` (optional, default: false): Annual renewal

**Example:**
```
POST /subscriptions/renew/123?isAnnual=true
```

**Response:**
```json
{
  "message": "Subscription renewed successfully"
}
```

---

### 8. Check Feature Access
**GET** `/subscriptions/can-use/{feature}`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Features:**
- `CV` - CV generation
- `API` - API calls
- `WATERMARK` - Watermark removal
- `DASHBOARD` - Dashboard access

**Example:**
```
GET /subscriptions/can-use/CV
```

**Response:**
```json
{
  "feature": "CV",
  "canUse": true
}
```

---

## Payment Endpoints

### 1. Initiate Payment
**POST** `/payments/initiate`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
- `planId` (required): Plan ID
- `isAnnual` (optional, default: false): Annual subscription
- `promoCode` (optional): Promo code

**Example:**
```
POST /payments/initiate?planId=2&isAnnual=true&promoCode=SAVE20
```

**Response:**
```json
{
  "paymentKey": "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5...",
  "message": "Payment initiated successfully"
}
```

**Next Steps:**
1. Redirect user to Paymob checkout with the payment key
2. User completes payment
3. Paymob sends webhook to `/payments/webhook`
4. Subscription is activated automatically

---

### 2. Payment Webhook (Paymob)
**POST** `/payments/webhook`

**Description:** This endpoint is called by Paymob after payment completion. It's a public endpoint (no authentication required).

**Request Body (from Paymob):**
```json
{
  "order_id": "12345",
  "payment_key": "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5...",
  "success": "true",
  "amount": 19999
}
```

**Response:**
```json
"Webhook processed successfully"
```

---

### 3. Verify Payment
**GET** `/payments/verify/{paymentId}`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "verified": true,
  "message": "Payment verified successfully"
}
```

---

## User Endpoints

### 1. Get Current User
**GET** `/users/me`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "ROLE_LICENSED_USER",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "isEmailVerified": true,
  "twoFactorEnabled": false,
  "lastLoginAt": "2025-01-17T10:30:00"
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid or expired promo code"
}
```

### 401 Unauthorized
```json
{
  "error": "Invalid credentials"
}
```

### 403 Forbidden
```json
{
  "error": "Access denied"
}
```

### 404 Not Found
```json
{
  "error": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "error": "An unexpected error occurred"
}
```

---

## Authentication

All protected endpoints require a JWT access token in the Authorization header:

```
Authorization: Bearer <access_token>
```

### Token Expiration
- **Access Token**: 15 minutes
- **Refresh Token**: 7 days

### Token Refresh Flow
1. Access token expires
2. Client receives 401 Unauthorized
3. Client calls `/auth/refresh` with refresh token
4. Server returns new access and refresh tokens
5. Client uses new access token for subsequent requests

---

## Rate Limiting

Rate limiting is applied based on user role:

- **Free Users (ROLE_USER)**: 100 requests/hour
- **Licensed Users (ROLE_LICENSED_USER)**: 1000 requests/hour
- **Admin Users (ROLE_ADMIN)**: Unlimited

---

## Subscription Status

### Status Values
- `PENDING`: Payment not completed
- `ACTIVE`: Subscription is active
- `EXPIRED`: Subscription has expired
- `CANCELLED`: Subscription was cancelled

### Status Transitions
```
PENDING → ACTIVE (after successful payment)
ACTIVE → EXPIRED (after end date)
ACTIVE → CANCELLED (manual cancellation)
PENDING → CANCELLED (payment not completed within 7 days)
```

---

## Usage Tracking

### CV Generation
```java
// Before generating CV
if (!subscriptionService.canUserUseFeature(userId, "CV")) {
    throw new BadRequestException("CV limit reached");
}

// After successful generation
subscriptionService.incrementUsage(userId, "CV");
```

### API Calls
```java
// Before API call
if (!subscriptionService.canUserUseFeature(userId, "API")) {
    throw new BadRequestException("API limit reached");
}

// After successful API call
subscriptionService.incrementUsage(userId, "API");
```

---

## Promo Codes

### Discount Types
- **PERCENTAGE**: Discount as percentage (e.g., 0.20 for 20% off)
- **FIXED_AMOUNT**: Fixed discount amount (e.g., 20.00 for $20 off)

### Validation Rules
- Must be active
- Must not be expired
- Must not exceed usage limit
- Purchase amount must meet minimum requirement

---

## Testing with cURL

### Register
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePassword123!"
  }'
```

### Get Plans
```bash
curl -X GET http://localhost:8080/api/v1/subscriptions/plans
```

### Initiate Payment
```bash
curl -X POST "http://localhost:8080/api/v1/payments/initiate?planId=2&isAnnual=true" \
  -H "Authorization: Bearer <access_token>"
```

### Get My Subscription
```bash
curl -X GET http://localhost:8080/api/v1/subscriptions/my-subscription \
  -H "Authorization: Bearer <access_token>"
```

---

## Postman Collection

Import the following JSON into Postman for easy API testing:

```json
{
  "info": {
    "name": "Civify API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api/v1"
    },
    {
      "key": "accessToken",
      "value": ""
    }
  ]
}
```

---

## Support

For API support, contact: support@civify.com
