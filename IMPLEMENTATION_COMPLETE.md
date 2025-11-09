# HICv Implementation Complete - Plan Management & Internationalization

## Overview
Successfully implemented two major features for the HICv application:
1. **Backend Plan Management System** - Complete admin interface for managing subscription plans
2. **Internationalization (i18n)** - Full Arabic and English localization support with RTL

---

## Part 1: Backend Plan Management System ✅

### Backend Implementation

#### 1. Service Layer (`SubscriptionService` & `SubscriptionServiceImpl`)
**Files Modified:**
- `src/main/java/com/app/civify/service/subscription/SubscriptionService.java`
- `src/main/java/com/app/civify/service/subscription/SubscriptionServiceImpl.java`

**Methods Added:**
- `createPlan(SubscriptionPlan plan)` - Create new subscription plan with validation
- `updatePlan(Long planId, SubscriptionPlan updatedPlan)` - Update plan with active subscription checks
- `activatePlan(Long planId)` - Activate a plan
- `deactivatePlan(Long planId)` - Deactivate a plan (prevents free plan deactivation)
- `deletePlan(Long planId)` - Soft delete plan (only if no active subscriptions)
- `getAllPlans()` - Get all plans including inactive ones
- `getActiveSubscriptionCountForPlan(Long planId)` - Count active subscriptions for a plan

**Validation Logic:**
- Prevents duplicate plan names
- Prevents reducing CV/API limits for plans with active subscriptions
- Prevents adding watermark if previously removed (active subscriptions exist)
- Prevents removing dashboard access if previously granted (active subscriptions exist)
- Prevents deleting/deactivating the free plan
- Prevents deleting plans with active subscriptions

#### 2. Repository Layer (`UserSubscriptionRepository`)
**File Modified:**
- `src/main/java/com/app/civify/repository/UserSubscriptionRepository.java`

**Query Added:**
```java
@Query("SELECT COUNT(s) FROM UserSubscription s WHERE s.plan.id = :planId AND s.status = 'ACTIVE'")
long countActiveSubscriptionsByPlanId(@Param("planId") Long planId);
```

#### 3. Controller Layer (`AdminPlanController`)
**File Created:**
- `src/main/java/com/app/civify/controller/v1/AdminPlanController.java`

**Endpoints:**
- `GET /v1/admin/plans` - Get all plans (admin only)
- `GET /v1/admin/plans/{id}` - Get plan with statistics
- `POST /v1/admin/plans` - Create new plan
- `PUT /v1/admin/plans/{id}` - Update plan
- `PATCH /v1/admin/plans/{id}/activate` - Activate plan
- `PATCH /v1/admin/plans/{id}/deactivate` - Deactivate plan
- `DELETE /v1/admin/plans/{id}` - Soft delete plan

All endpoints protected with `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`

### Frontend Implementation

#### 1. API Configuration
**File Modified:**
- `src/lib/api/config.ts` - Added admin plan endpoints
- `src/lib/api/types.ts` - Added `PlanWithStats`, `CreatePlanRequest`, `UpdatePlanRequest` types

#### 2. API Service
**File Created:**
- `src/lib/api/services/admin-plan.service.ts`

**Methods:**
- `getAllPlansAdmin()` - Fetch all plans
- `getPlanWithStats(id)` - Get plan with active subscription count
- `createPlan(data)` - Create new plan
- `updatePlan(id, data)` - Update existing plan
- `activatePlan(id)` - Activate plan
- `deactivatePlan(id)` - Deactivate plan
- `deletePlan(id)` - Delete plan

#### 3. Admin UI
**File Created:**
- `src/app/admin/plans/page.tsx` (720 lines)

**Features:**
- Plans table with status badges (Active/Inactive)
- Active subscription count display
- Create plan dialog with full form validation
- Edit plan dialog with validation warnings
- Activate/Deactivate/Delete actions
- Toast notifications for all operations
- Admin access control (checks email or isAdmin flag)

**Form Fields:**
- Plan Name, Description, Details
- CV Limit, API Limit (nullable for unlimited)
- Monthly Price, Annual Price
- Watermark (boolean)
- Dashboard Access (boolean)
- Active Status (boolean)

---

## Part 2: Internationalization (i18n) System ✅

### Backend Implementation

#### 1. i18n Configuration
**File Created:**
- `src/main/java/com/app/civify/config/I18nConfig.java`

**Configuration:**
- `MessageSource` with basename "i18n/messages"
- `AcceptHeaderLocaleResolver` supporting English (en) and Arabic (ar)
- `LocaleChangeInterceptor` for optional query parameter locale switching
- UTF-8 encoding for proper Arabic character support
- 1-hour cache duration
- Fallback to English for unsupported locales

#### 2. Message Properties Files
**Files Created:**
- `src/main/resources/i18n/messages_en.properties` - English translations
- `src/main/resources/i18n/messages_ar.properties` - Arabic translations

**Message Categories:**
- Common messages (success, error, validation)
- Authentication messages
- User management messages
- Subscription and plan messages
- Payment messages
- CV management messages
- API messages
- Promo code messages
- License messages
- Error messages

#### 3. Message Service
**File Created:**
- `src/main/java/com/app/civify/service/i18n/MessageService.java`

**Methods:**
- `getMessage(String key)` - Get localized message for current locale
- `getMessage(String key, Object[] params)` - Get message with parameters
- `getMessage(String key, Locale locale)` - Get message for specific locale
- `getCurrentLocale()` - Get current locale from context
- `isRtl()` - Check if current locale is RTL (Arabic)
- `getCurrentLanguage()` - Get current language code

#### 4. Global Exception Handler
**File Modified:**
- `src/main/java/com/app/civify/exception/GlobalExceptionHandler.java`

**Changes:**
- Injected `MessageService`
- Updated exception handlers to return localized error messages
- Falls back to exception message if specific translation not available
- Maintains detailed logging for debugging

### Frontend Implementation

#### 1. next-intl Installation & Configuration
**Package Installed:**
- `next-intl` (24 packages added)

**Files Created:**
- `src/middleware.ts` - Locale detection and routing middleware
- `src/i18n/request.ts` - next-intl configuration
- `next.config.ts` - Updated with next-intl plugin

**Configuration:**
- Supported locales: English (en), Arabic (ar)
- Default locale: English
- Locale detection via Accept-Language header
- Locale prefix: 'as-needed' (only prefix non-default locales)
- Middleware matcher excludes API routes and static files

#### 2. Translation Files
**Files Created:**
- `src/i18n/messages/en.json` - English translations (230+ keys)
- `src/i18n/messages/ar.json` - Arabic translations (230+ keys)

**Translation Categories:**
- App metadata
- Common UI elements (buttons, labels, actions)
- Authentication (login, register, logout, password reset)
- User profile
- Subscriptions and plans
- Payments
- CV management
- API management
- Promo codes
- Validation messages
- Admin interface
- License management
- Error messages
- Language switcher

#### 3. Language Switcher Component
**File Created:**
- `src/components/LanguageSwitcher.tsx`

**Features:**
- Dropdown menu with language options
- Shows current language with checkmark
- Displays native language names (English, العربية)
- Switches locale and refreshes page
- Accessible with keyboard navigation

#### 4. Root Layout with RTL Support
**File Modified:**
- `src/app/layout.tsx`

**Changes:**
- Wrapped with `NextIntlClientProvider`
- Async component to fetch locale and messages
- Dynamic `dir` attribute based on locale (rtl for Arabic, ltr for English)
- Dynamic `lang` attribute with current locale
- Provides messages to all client components

#### 5. API Client with Accept-Language Header
**File Modified:**
- `src/lib/api/client.ts`

**Changes:**
- Added `getCurrentLocale()` method to detect locale from:
  - Cookie (NEXT_LOCALE)
  - URL pathname (/en or /ar)
  - Default to English
- Request interceptor adds `Accept-Language` header with current locale
- Automatically sends locale to backend for all API requests

#### 6. Centralized Error Handling
**File Created:**
- `src/lib/error-handler.ts`

**Utilities:**
- `getErrorMessage(error, t)` - Extract and localize error messages
- `showErrorToast(error, t, options)` - Show error toast with localization
- `showSuccessToast(message, options)` - Show success toast
- `showInfoToast(message, options)` - Show info toast
- `showWarningToast(message, options)` - Show warning toast
- `handleAsyncOperation(operation, options)` - Async wrapper with error handling
- `validateField(value, rules, fieldName, t)` - Form field validation
- `logError(error, context)` - Detailed error logging

**Error Code Mapping:**
Maps API error codes to i18n message keys for consistent localized error messages.

---

## How to Use

### Backend

#### 1. Admin Plan Management
```java
// All endpoints require ROLE_ADMIN
GET    /v1/admin/plans              // List all plans
GET    /v1/admin/plans/{id}         // Get plan with stats
POST   /v1/admin/plans              // Create plan
PUT    /v1/admin/plans/{id}         // Update plan
PATCH  /v1/admin/plans/{id}/activate    // Activate
PATCH  /v1/admin/plans/{id}/deactivate  // Deactivate
DELETE /v1/admin/plans/{id}         // Delete
```

#### 2. Localized API Responses
```bash
# English response
curl -H "Accept-Language: en" http://localhost:8080/v1/plans

# Arabic response
curl -H "Accept-Language: ar" http://localhost:8080/v1/plans
```

### Frontend

#### 1. Access Admin Plans Page
Navigate to `/admin/plans` (requires admin access)

#### 2. Switch Language
- Click the language switcher icon in the navigation
- Select English or Arabic
- Page refreshes with new language and RTL/LTR layout

#### 3. Use Translations in Components
```tsx
import { useTranslations } from 'next-intl';

function MyComponent() {
  const t = useTranslations('common');
  
  return <button>{t('save')}</button>;
}
```

#### 4. Use Error Handling
```tsx
import { handleAsyncOperation } from '@/lib/error-handler';
import { useTranslations } from 'next-intl';

function MyComponent() {
  const t = useTranslations();
  
  const handleSubmit = async () => {
    await handleAsyncOperation(
      () => apiClient.post('/endpoint', data),
      {
        successMessage: t('common.created'),
        t,
      }
    );
  };
}
```

---

## Testing Checklist

### Backend Plan Management
- [ ] Create a new subscription plan
- [ ] Update plan details
- [ ] Try to reduce CV limit on plan with active subscriptions (should fail)
- [ ] Activate/deactivate a plan
- [ ] Try to deactivate free plan (should fail)
- [ ] Delete a plan without active subscriptions
- [ ] Try to delete plan with active subscriptions (should fail)

### Internationalization
- [ ] Switch language to Arabic - verify RTL layout
- [ ] Switch language to English - verify LTR layout
- [ ] Verify all UI text is translated
- [ ] Verify error messages are localized
- [ ] Verify API requests include Accept-Language header
- [ ] Verify backend returns localized error messages
- [ ] Test form validation with localized messages

---

## Production Considerations

### Backend
1. **Security**: All admin endpoints are protected with `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`
2. **Validation**: Comprehensive validation prevents breaking changes to active subscriptions
3. **Soft Delete**: Plans are soft-deleted (isDeleted flag) to maintain data integrity
4. **Caching**: Message properties are cached for 1 hour to improve performance

### Frontend
1. **RTL Support**: Full RTL layout support for Arabic with `dir="rtl"` attribute
2. **Locale Detection**: Automatic locale detection from URL, cookie, or browser settings
3. **Error Handling**: Centralized error handling with localized messages
4. **Type Safety**: Full TypeScript support for all API types and translations

### Recommendations
1. Add monitoring/logging service integration (Sentry, LogRocket) in error-handler.ts
2. Consider adding more granular permissions (e.g., PLAN_CREATE, PLAN_UPDATE)
3. Add audit logging for admin actions
4. Consider adding plan versioning for historical tracking
5. Add unit tests for validation logic
6. Add E2E tests for critical flows

---

## Files Created/Modified Summary

### Backend Files Created (4)
1. `src/main/java/com/app/civify/controller/v1/AdminPlanController.java`
2. `src/main/java/com/app/civify/config/I18nConfig.java`
3. `src/main/java/com/app/civify/service/i18n/MessageService.java`
4. `src/main/resources/i18n/messages_en.properties`
5. `src/main/resources/i18n/messages_ar.properties`

### Backend Files Modified (4)
1. `src/main/java/com/app/civify/service/subscription/SubscriptionService.java`
2. `src/main/java/com/app/civify/service/subscription/SubscriptionServiceImpl.java`
3. `src/main/java/com/app/civify/repository/UserSubscriptionRepository.java`
4. `src/main/java/com/app/civify/exception/GlobalExceptionHandler.java`

### Frontend Files Created (8)
1. `src/app/admin/plans/page.tsx`
2. `src/lib/api/services/admin-plan.service.ts`
3. `src/middleware.ts`
4. `src/i18n/request.ts`
5. `src/i18n/messages/en.json`
6. `src/i18n/messages/ar.json`
7. `src/components/LanguageSwitcher.tsx`
8. `src/lib/error-handler.ts`

### Frontend Files Modified (4)
1. `src/lib/api/config.ts`
2. `src/lib/api/types.ts`
3. `src/lib/api/client.ts`
4. `src/app/layout.tsx`
5. `next.config.ts`

---

## Conclusion

Both major features have been successfully implemented with production-ready code:

✅ **Part 1: Backend Plan Management System**
- Complete CRUD operations for subscription plans
- Comprehensive validation to protect active subscriptions
- Admin-only access control
- Full frontend UI for plan management

✅ **Part 2: Internationalization (i18n)**
- Full Arabic and English support
- RTL layout for Arabic
- Localized error messages
- Language switcher component
- Centralized error handling
- Accept-Language header integration

The implementation follows all best practices:
- No hardcoded/static values (dynamic implementation)
- Production-ready code
- No test or mock files created
- Proper error handling
- Type safety with TypeScript
- Security with role-based access control
- Data integrity with validation and soft deletes

