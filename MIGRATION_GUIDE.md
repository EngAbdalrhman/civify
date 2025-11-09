# Migration Guide: From License System to Subscription System

## Overview

This guide helps developers migrate from the old license-based system to the new subscription-based system with backend integration.

## Breaking Changes

### 1. License System Removed

**Before:**
```typescript
// Old license check
if (profile.isLicensed) {
  // Allow access
}

// Old license activation
await databaseService.activateLicense(userId, licenseKey);
```

**After:**
```typescript
// New subscription check
import { subscriptionService } from '@/lib/api';

const subscription = await subscriptionService.getMyActiveSubscription();
if (subscription && subscription.status === 'ACTIVE') {
  // Allow access
}

// New subscription activation (via payment)
const payment = await paymentService.initiatePayment(planId, isAnnual);
window.location.href = payment.paymentUrl;
```

### 2. User Profile Changes

**Before:**
```typescript
interface UserProfile {
  isLicensed: boolean;
  licenseKey?: string;
  licenseActivatedAt?: string;
}
```

**After:**
```typescript
interface UserProfile {
  plan?: "free" | "premium" | "enterprise" | "lifetime";
  // License fields removed
}

// Use backend subscription instead
const subscription = await subscriptionService.getMyActiveSubscription();
```

### 3. API Routes Removed

**Before:**
```typescript
// Old mock API routes
POST /api/cv/generate-masked
POST /api/cv/generate-tailored
GET /api/health
POST /api/payment/create
```

**After:**
```typescript
// Use backend API
import { apiClient } from '@/lib/api';

// CV operations will use backend endpoints
POST /apis/v1/cvs/upload
POST /apis/v1/cvs/{id}/tailor

// Payment uses backend
POST /apis/v1/payments/initiate
```

## Migration Steps

### Step 1: Update Authentication

**Before:**
```typescript
// Firebase auth only
import { auth } from '@/lib/firebase';
const user = auth.currentUser;
```

**After:**
```typescript
// Firebase auth + Backend JWT
import { auth } from '@/lib/firebase';
import { authService } from '@/lib/api';

// After Firebase login, login to backend
const firebaseUser = auth.currentUser;
if (firebaseUser) {
  const idToken = await firebaseUser.getIdToken();
  // Backend will verify Firebase token and issue JWT
  await authService.login({ email: firebaseUser.email, password: idToken });
}
```

### Step 2: Update License Checks

**Before:**
```typescript
const profile = await databaseService.getUserProfile(user);
if (!profile.isLicensed) {
  router.push('/dashboard/license');
  return;
}
```

**After:**
```typescript
import { subscriptionService } from '@/lib/api';

const subscription = await subscriptionService.getMyActiveSubscription();
if (!subscription || subscription.status !== 'ACTIVE') {
  router.push('/dashboard/plans');
  return;
}
```

### Step 3: Update Feature Access Checks

**Before:**
```typescript
// Check if user can generate CV
if (profile.cvCount >= profile.cvLimit) {
  toast({ title: "Limit reached", description: "Upgrade to generate more CVs" });
  return;
}
```

**After:**
```typescript
import { subscriptionService } from '@/lib/api';

// Check if user can use CV feature
const canUseCV = await subscriptionService.canUseFeature('cv');
if (!canUseCV) {
  toast({ title: "Limit reached", description: "Upgrade to generate more CVs" });
  return;
}

// Or get remaining count
const remaining = await subscriptionService.getRemainingCvGenerations();
if (remaining === 0) {
  toast({ title: "Limit reached" });
  return;
}
```

### Step 4: Update Payment Flow

**Before:**
```typescript
// PayPal only
const response = await fetch('/api/payment/create', {
  method: 'POST',
  body: JSON.stringify({ planId }),
});
```

**After:**
```typescript
import { paymentService } from '@/lib/api';

// Paymob integration
const payment = await paymentService.initiatePayment(planId, isAnnual, promoCode);
window.location.href = payment.paymentUrl;
```

### Step 5: Update Admin Dashboard

**Before:**
```typescript
// License management
const licenses = await adminDatabase.getAllLicenses();
await adminDatabase.grantLicense(userId);
await adminDatabase.revokeLicense(licenseId);
```

**After:**
```typescript
// Subscription management (via backend API)
import { subscriptionService } from '@/lib/api';

const subscriptions = await subscriptionService.getMySubscriptions();
// Admin operations should use backend admin endpoints
```

## Code Examples

### Example 1: Protected Route

**Before:**
```typescript
export default function ProtectedPage() {
  const { user } = useAuth();
  const databaseService = useDatabase();
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    const userProfile = await databaseService.getUserProfile(user);
    if (!userProfile.isLicensed) {
      router.push('/dashboard/license');
      return;
    }
    setProfile(userProfile);
  };

  // ...
}
```

**After:**
```typescript
import { subscriptionService } from '@/lib/api';

export default function ProtectedPage() {
  const { user } = useAuth();
  const [subscription, setSubscription] = useState(null);

  useEffect(() => {
    loadSubscription();
  }, []);

  const loadSubscription = async () => {
    const sub = await subscriptionService.getMyActiveSubscription();
    if (!sub || sub.status !== 'ACTIVE') {
      router.push('/dashboard/plans');
      return;
    }
    setSubscription(sub);
  };

  // ...
}
```

### Example 2: Feature Usage

**Before:**
```typescript
const handleGenerateCV = async () => {
  const profile = await databaseService.getUserProfile(user);
  
  if (profile.cvCount >= profile.cvLimit) {
    toast({ title: "Limit reached" });
    return;
  }

  // Generate CV
  await generateCV();

  // Update count
  await databaseService.updateUserProfile(user.uid, {
    cvCount: profile.cvCount + 1,
  });
};
```

**After:**
```typescript
import { subscriptionService } from '@/lib/api';

const handleGenerateCV = async () => {
  const canUse = await subscriptionService.canUseFeature('cv');
  
  if (!canUse) {
    toast({ title: "Limit reached" });
    return;
  }

  // Generate CV (backend will track usage)
  await generateCV();
};
```

### Example 3: Plan Upgrade

**Before:**
```typescript
const handleUpgrade = async (planId: string) => {
  // Show PayPal button
  // Manual license activation after payment
};
```

**After:**
```typescript
import { paymentService } from '@/lib/api';

const handleUpgrade = async (planId: number) => {
  try {
    const payment = await paymentService.initiatePayment(planId, false);
    window.location.href = payment.paymentUrl;
  } catch (error) {
    toast({ title: "Error", description: error.message });
  }
};
```

## Environment Variables

Update your `.env.local` file:

```env
# Add these new variables
NEXT_PUBLIC_BACKEND_API_URL=http://localhost:8077/apis
NEXT_PUBLIC_PAYMOB_ENABLED=true
NEXT_PUBLIC_PAYMOB_IFRAME_ID=your_iframe_id
NEXT_PUBLIC_PAYMOB_HMAC_SECRET=your_hmac_secret

# Keep existing Firebase and Appwrite variables
NEXT_PUBLIC_FIREBASE_API_KEY=...
NEXT_PUBLIC_APPWRITE_ENDPOINT=...
```

## Testing Checklist

- [ ] Authentication flow works with backend
- [ ] Subscription plans load from backend
- [ ] Payment flow completes successfully
- [ ] Subscription activation works
- [ ] Feature access checks work correctly
- [ ] Admin dashboard shows subscriptions
- [ ] Webhook handlers work for payment callbacks
- [ ] Token refresh works automatically
- [ ] Error handling displays user-friendly messages
- [ ] Logout clears tokens properly

## Common Issues

### Issue 1: CORS Errors

**Problem:** API requests fail with CORS errors

**Solution:** Configure CORS in backend `application.properties`:
```properties
spring.web.cors.allowed-origins=http://localhost:3000
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
```

### Issue 2: Token Not Attached

**Problem:** API requests return 401 even after login

**Solution:** Check token storage:
```typescript
import { tokenManager } from '@/lib/api';

console.log('Access Token:', tokenManager.getAccessToken());
console.log('Is Authenticated:', tokenManager.isAuthenticated());
```

### Issue 3: Payment Redirect Fails

**Problem:** Payment doesn't redirect to Paymob

**Solution:** Check environment variables:
```typescript
console.log('Paymob Enabled:', process.env.NEXT_PUBLIC_PAYMOB_ENABLED);
console.log('Iframe ID:', process.env.NEXT_PUBLIC_PAYMOB_IFRAME_ID);
```

## Rollback Plan

If you need to rollback:

1. Restore deleted files from git:
   ```bash
   git checkout HEAD~1 -- src/app/dashboard/license/
   git checkout HEAD~1 -- src/lib/utils/license.ts
   git checkout HEAD~1 -- src/app/api/cv/
   ```

2. Revert modified files:
   ```bash
   git checkout HEAD~1 -- src/lib/types.ts
   git checkout HEAD~1 -- src/lib/services/
   ```

3. Remove new API integration:
   ```bash
   rm -rf src/lib/api/
   rm -rf src/app/dashboard/checkout/
   rm -rf src/app/dashboard/payment/
   ```

## Support

For help with migration:
1. Check `IMPLEMENTATION_SUMMARY.md` for architecture overview
2. Review `src/lib/api/README.md` for API usage
3. Contact the development team

---

**Last Updated:** 2025-11-09  
**Version:** 1.0.0

