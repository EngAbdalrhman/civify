# HICv Frontend - Backend Integration & Paymob Payment Implementation

## Overview

This document summarizes the comprehensive implementation of backend API integration and Paymob payment gateway for the HICv frontend application.

## Completed Tasks

### ✅ Task 1: Remove License Module (COMPLETED)

All license-related code has been removed from the frontend:

**Files Deleted:**
- `src/app/dashboard/license/page.tsx` - License activation page
- `src/lib/utils/license.ts` - License utilities

**Files Modified:**
- `src/lib/types.ts` - Removed license types, added Subscription types
- `src/lib/services/appwrite-database.ts` - Removed license methods
- `src/lib/services/appwrite-admin-database.ts` - Removed license management
- `src/app/admin/dashboard/page.tsx` - Replaced license UI with subscription UI
- `src/app/dashboard/api-keys/page.tsx` - Removed license checks
- `src/components/layout/Navbar.tsx` - Replaced "Activate License" with "Plans"
- `src/app/api/payment/webhook/paypal/route.ts` - Removed license field

### ✅ Task 2: Remove Mock API Routes (COMPLETED)

All mock Next.js API routes have been removed:

**Files Deleted:**
- `src/app/api/cv/generate-masked/route.ts`
- `src/app/api/cv/generate-tailored/route.ts`
- `src/app/api/health/route.ts`
- `src/app/api/payment/create/route.ts`

**Files Kept:**
- `src/app/api/payment/webhook/paymob/route.ts` - Webhook handler
- `src/app/api/payment/webhook/paypal/route.ts` - Webhook handler

### ✅ Task 3: Implement Paymob Payment Integration (COMPLETED)

Complete Paymob payment integration with checkout flow:

**New Files Created:**
- `src/app/dashboard/checkout/page.tsx` - Checkout page with plan summary and payment details
- `src/app/dashboard/payment/success/page.tsx` - Payment success callback page
- `src/app/dashboard/payment/failure/page.tsx` - Payment failure callback page
- `src/lib/api/services/payment.service.ts` - Payment service with Paymob integration

**Features:**
- Plan selection and checkout flow
- Annual/monthly billing toggle with savings calculation
- Promo code support
- Paymob iframe integration
- Payment verification with backend
- HMAC signature validation
- Success/failure callback handling

### ✅ Task 4: Implement Backend Integration Structure (COMPLETED)

Complete backend API client infrastructure:

**New Files Created:**

1. **Configuration & Types:**
   - `src/lib/api/config.ts` - API endpoints, constants, and configuration
   - `src/lib/api/types.ts` - TypeScript interfaces for all DTOs

2. **Core Infrastructure:**
   - `src/lib/api/client.ts` - Axios HTTP client with interceptors
   - `src/lib/api/token-manager.ts` - JWT token management and storage

3. **Service Layer:**
   - `src/lib/api/services/auth.service.ts` - Authentication (login, register, 2FA, logout)
   - `src/lib/api/services/subscription.service.ts` - Subscription management
   - `src/lib/api/services/payment.service.ts` - Payment processing
   - `src/lib/api/services/user.service.ts` - User management
   - `src/lib/api/services/index.ts` - Service exports

4. **Documentation:**
   - `src/lib/api/README.md` - Comprehensive API integration guide
   - `src/lib/api/index.ts` - Main export file

**Files Modified:**
- `src/app/dashboard/plans/page.tsx` - Integrated with backend subscription API
- `package.json` - Added axios dependency

**Configuration Files:**
- `.env.example` - Environment variable template

## Architecture

### API Client Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend Application                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Auth Service │  │ Subscription │  │   Payment    │      │
│  │              │  │   Service    │  │   Service    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│                    ┌───────▼────────┐                        │
│                    │   API Client   │                        │
│                    │  (Axios + JWT) │                        │
│                    └───────┬────────┘                        │
│                            │                                 │
│                    ┌───────▼────────┐                        │
│                    │ Token Manager  │                        │
│                    │  (JWT Storage) │                        │
│                    └────────────────┘                        │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│                      HTTP/HTTPS                              │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│              Java Spring Boot Backend API                    │
│              (http://localhost:8077/apis/v1)                 │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Key Features

1. **Automatic Token Management:**
   - JWT tokens stored securely in localStorage
   - Automatic token refresh before expiry (5-minute threshold)
   - Automatic retry on 401 errors with refreshed token
   - Token expiry detection and validation

2. **HTTP Interceptors:**
   - Request interceptor: Attaches JWT token to all requests
   - Response interceptor: Handles errors and token refresh
   - Automatic redirect to login on authentication failure

3. **Service-Oriented Architecture:**
   - Clean separation of concerns
   - Domain-specific services (auth, subscription, payment, user)
   - Singleton pattern for service instances
   - Full TypeScript type safety

4. **Error Handling:**
   - Custom `ApiException` class
   - Structured error responses
   - Network error detection
   - Timeout handling

5. **Paymob Integration:**
   - Payment initiation with backend
   - Iframe-based payment flow
   - HMAC signature validation
   - Success/failure callback handling
   - Transaction verification

## Payment Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Plans   │────▶│ Checkout │────▶│  Paymob  │────▶│ Success/ │
│   Page   │     │   Page   │     │  Iframe  │     │ Failure  │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
     │                │                  │                │
     │                │                  │                │
     ▼                ▼                  ▼                ▼
  Select Plan    Initiate Payment   Complete Payment  Verify &
  from Backend   with Backend       on Paymob         Activate
  API            Get Payment Key    Gateway           Subscription
```

### Detailed Payment Steps:

1. **Plan Selection** (`/dashboard/plans`):
   - Load plans from backend API
   - Display plan features and pricing
   - User selects a plan
   - Redirect to checkout

2. **Checkout** (`/dashboard/checkout`):
   - Display plan summary
   - Choose billing cycle (monthly/annual)
   - Enter promo code (optional)
   - Calculate total price
   - Initiate payment with backend
   - Receive payment key and Paymob URL

3. **Payment** (Paymob Iframe):
   - Redirect to Paymob payment page
   - User enters payment details
   - Paymob processes payment
   - Redirect to success/failure page

4. **Callback** (`/dashboard/payment/success` or `/failure`):
   - Parse Paymob callback parameters
   - Verify payment with backend
   - Activate subscription
   - Display confirmation

## Environment Variables

Required environment variables (see `.env.example`):

```env
# Backend API
NEXT_PUBLIC_BACKEND_API_URL=http://localhost:8077/apis

# Paymob
NEXT_PUBLIC_PAYMOB_ENABLED=true
NEXT_PUBLIC_PAYMOB_IFRAME_ID=your_iframe_id
NEXT_PUBLIC_PAYMOB_HMAC_SECRET=your_hmac_secret
```

## Usage Examples

### Authentication

```typescript
import { authService } from '@/lib/api';

// Login
const response = await authService.login({
  email: 'user@example.com',
  password: 'password123'
});

// Check if authenticated
if (authService.isAuthenticated()) {
  // User is logged in
}

// Logout
await authService.logout();
```

### Subscription Management

```typescript
import { subscriptionService } from '@/lib/api';

// Get all plans
const plans = await subscriptionService.getAllPlans();

// Get active subscription
const subscription = await subscriptionService.getMyActiveSubscription();

// Check feature access
const canUseCV = await subscriptionService.canUseFeature('cv');
```

### Payment Processing

```typescript
import { paymentService } from '@/lib/api';

// Initiate payment
const payment = await paymentService.initiatePayment(
  planId,
  isAnnual,
  promoCode
);

// Redirect to Paymob
window.location.href = payment.paymentUrl;
```

## Testing

To test the implementation:

1. **Start the backend:**
   ```bash
   cd backend/civify
   ./mvnw spring-boot:run
   ```

2. **Start the frontend:**
   ```bash
   npm run dev
   ```

3. **Test the flow:**
   - Navigate to `/dashboard/plans`
   - Select a plan
   - Complete checkout
   - Test payment (use Paymob test cards)
   - Verify subscription activation

## Dependencies Added

- `axios` (v1.6.0+) - HTTP client for API requests

## Next Steps

1. **Backend Configuration:**
   - Ensure backend is running on `http://localhost:8077`
   - Configure Paymob credentials in backend
   - Set up subscription plans in database

2. **Frontend Configuration:**
   - Copy `.env.example` to `.env.local`
   - Fill in environment variables
   - Configure Paymob iframe ID and HMAC secret

3. **Testing:**
   - Test authentication flow
   - Test subscription plan loading
   - Test payment flow with Paymob test cards
   - Verify webhook handling

4. **Production Deployment:**
   - Use HTTPS for all API calls
   - Consider httpOnly cookies for token storage
   - Set up proper CORS configuration
   - Configure production Paymob credentials
   - Set up monitoring and error tracking

## Security Considerations

1. **Token Storage:**
   - Currently using localStorage
   - Consider httpOnly cookies for production
   - Implement token rotation

2. **API Security:**
   - All requests use JWT authentication
   - HTTPS required in production
   - HMAC validation for payment callbacks

3. **Error Handling:**
   - Never expose sensitive error details to users
   - Log errors securely on backend
   - Implement rate limiting

## Support

For issues or questions:
- Check `src/lib/api/README.md` for detailed API documentation
- Review backend API documentation
- Contact the development team

---

**Implementation Date:** 2025-11-09  
**Status:** ✅ COMPLETE  
**Version:** 1.0.0

