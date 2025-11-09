# Translation Examples - Using `translate()` Function

This guide shows how to use the improved `translate()` function instead of the generic `t()` function.

## Why `translate()` instead of `t()`?

The `translate()` function is more descriptive and clearly indicates what it does. It's an alias for `useTranslations()` from next-intl but with a better name.

## Basic Usage

### Client Components

```tsx
"use client";

import { useTranslate } from '@/lib/i18n/translate';

export function MyComponent() {
  // Get translation function for a specific namespace
  const translate = useTranslate('common');
  
  return (
    <div>
      <button>{translate('save')}</button>
      <button>{translate('cancel')}</button>
      <button>{translate('delete')}</button>
    </div>
  );
}
```

### Server Components

```tsx
import { getTranslate } from '@/lib/i18n/translate';

export default async function MyPage() {
  // Get translation function for a specific namespace
  const translate = await getTranslate('common');
  
  return (
    <div>
      <h1>{translate('title')}</h1>
      <p>{translate('description')}</p>
    </div>
  );
}
```

## Using Translation Namespaces

### With Namespace Helper

```tsx
import { useTranslate, TranslationNamespace } from '@/lib/i18n/translate';

export function LoginForm() {
  // Use the namespace helper for type safety and autocomplete
  const translate = useTranslate(TranslationNamespace.auth.login);
  
  return (
    <form>
      <h1>{translate('title')}</h1>
      <input placeholder={translate('email')} />
      <input type="password" placeholder={translate('password')} />
      <button>{translate('submit')}</button>
      <a href="/forgot-password">{translate('forgotPassword')}</a>
    </form>
  );
}
```

### Multiple Namespaces in One Component

```tsx
import { useTranslate } from '@/lib/i18n/translate';

export function UserProfile() {
  const commonTranslate = useTranslate('common');
  const userTranslate = useTranslate('user.profile');
  
  return (
    <div>
      <h1>{userTranslate('title')}</h1>
      <button>{commonTranslate('save')}</button>
      <button>{commonTranslate('cancel')}</button>
    </div>
  );
}
```

## Translation with Parameters

```tsx
import { useTranslate } from '@/lib/i18n/translate';

export function ValidationExample() {
  const translate = useTranslate('validation');
  
  // Translation with parameters
  const emailError = translate('required', { field: 'Email' });
  // English: "Email is required"
  // Arabic: "البريد الإلكتروني مطلوب"
  
  const passwordError = translate('min', { field: 'Password', min: 8 });
  // English: "Password must be at least 8 characters"
  // Arabic: "كلمة المرور يجب أن تكون على الأقل 8"
  
  return (
    <div>
      <p className="text-red-500">{emailError}</p>
      <p className="text-red-500">{passwordError}</p>
    </div>
  );
}
```

## Using with Error Handler

### Basic Error Handling

```tsx
"use client";

import { useTranslate } from '@/lib/i18n/translate';
import { showErrorToast, showSuccessToast } from '@/lib/error-handler';
import { apiClient } from '@/lib/api/client';

export function CreatePlanForm() {
  const translate = useTranslate();
  
  const handleSubmit = async (data: any) => {
    try {
      await apiClient.post('/admin/plans', data);
      
      // Show success toast with translation
      showSuccessToast(
        translate('admin.plans.created'),
        { title: translate('common.success') }
      );
    } catch (error) {
      // Show error toast with translation
      showErrorToast(
        error,
        translate,
        { title: translate('common.error') }
      );
    }
  };
  
  return <form onSubmit={handleSubmit}>{/* Form fields */}</form>;
}
```

### Using handleAsyncOperation

```tsx
"use client";

import { useTranslate } from '@/lib/i18n/translate';
import { handleAsyncOperation } from '@/lib/error-handler';
import { apiClient } from '@/lib/api/client';

export function DeletePlanButton({ planId }: { planId: number }) {
  const translate = useTranslate();
  
  const handleDelete = async () => {
    const result = await handleAsyncOperation(
      () => apiClient.delete(`/admin/plans/${planId}`),
      {
        successMessage: translate('admin.plans.deleted'),
        errorTitle: translate('common.error'),
        translate, // Pass the translate function
        onSuccess: () => {
          // Refresh the page or update state
          window.location.reload();
        },
      }
    );
  };
  
  return (
    <button onClick={handleDelete}>
      {translate('common.delete')}
    </button>
  );
}
```

## Form Validation with Translation

```tsx
"use client";

import { useState } from 'react';
import { useTranslate } from '@/lib/i18n/translate';
import { validateField } from '@/lib/error-handler';

export function PlanForm() {
  const translate = useTranslate();
  const [errors, setErrors] = useState<Record<string, string>>({});
  
  const handleValidation = (name: string, value: string) => {
    let error: string | null = null;
    
    if (name === 'planName') {
      error = validateField(
        value,
        { required: true, min: 3, max: 50 },
        translate('admin.plans.planName'),
        translate
      );
    } else if (name === 'email') {
      error = validateField(
        value,
        { required: true, email: true },
        translate('auth.login.email'),
        translate
      );
    }
    
    setErrors(prev => ({
      ...prev,
      [name]: error || '',
    }));
  };
  
  return (
    <form>
      <div>
        <label>{translate('admin.plans.planName')}</label>
        <input
          name="planName"
          onChange={(e) => handleValidation('planName', e.target.value)}
        />
        {errors.planName && (
          <span className="text-red-500">{errors.planName}</span>
        )}
      </div>
    </form>
  );
}
```

## RTL Support

### Check if RTL is Active

```tsx
import { useTranslate, useIsRtl, useCurrentLocale } from '@/lib/i18n/translate';

export function DirectionalComponent() {
  const translate = useTranslate('common');
  const isRtl = useIsRtl();
  const locale = useCurrentLocale();
  
  return (
    <div className={isRtl ? 'text-right' : 'text-left'}>
      <p>Current locale: {locale}</p>
      <p>Is RTL: {isRtl ? 'Yes' : 'No'}</p>
      <p>{translate('title')}</p>
    </div>
  );
}
```

## Complete Example: Admin Plans Page

```tsx
"use client";

import { useState, useEffect } from 'react';
import { useTranslate, TranslationNamespace } from '@/lib/i18n/translate';
import { handleAsyncOperation, showSuccessToast } from '@/lib/error-handler';
import { adminPlanService } from '@/lib/api/services/admin-plan.service';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

export default function AdminPlansPage() {
  // Get translation functions for different namespaces
  const commonTranslate = useTranslate(TranslationNamespace.common);
  const adminTranslate = useTranslate(TranslationNamespace.admin.plans);
  const translate = useTranslate(); // Root namespace for error handling
  
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Load plans
  useEffect(() => {
    loadPlans();
  }, []);
  
  const loadPlans = async () => {
    setLoading(true);
    const result = await handleAsyncOperation(
      () => adminPlanService.getAllPlansAdmin(),
      {
        translate,
        onSuccess: (data) => setPlans(data),
      }
    );
    setLoading(false);
  };
  
  const handleActivate = async (planId: number) => {
    await handleAsyncOperation(
      () => adminPlanService.activatePlan(planId),
      {
        successMessage: adminTranslate('activated'),
        translate,
        onSuccess: () => loadPlans(),
      }
    );
  };
  
  const handleDeactivate = async (planId: number) => {
    await handleAsyncOperation(
      () => adminPlanService.deactivatePlan(planId),
      {
        successMessage: adminTranslate('deactivated'),
        translate,
        onSuccess: () => loadPlans(),
      }
    );
  };
  
  const handleDelete = async (planId: number) => {
    if (!confirm(adminTranslate('confirmDelete'))) return;
    
    await handleAsyncOperation(
      () => adminPlanService.deletePlan(planId),
      {
        successMessage: adminTranslate('deleted'),
        translate,
        onSuccess: () => loadPlans(),
      }
    );
  };
  
  return (
    <div className="container mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">{adminTranslate('title')}</h1>
        <LanguageSwitcher />
      </div>
      
      {loading ? (
        <p>{commonTranslate('loading')}</p>
      ) : (
        <div className="space-y-4">
          {plans.map((plan: any) => (
            <div key={plan.id} className="border p-4 rounded">
              <h2 className="text-xl font-semibold">{plan.name}</h2>
              <p>{plan.description}</p>
              
              <div className="mt-4 space-x-2">
                {plan.active ? (
                  <button onClick={() => handleDeactivate(plan.id)}>
                    {commonTranslate('deactivate')}
                  </button>
                ) : (
                  <button onClick={() => handleActivate(plan.id)}>
                    {commonTranslate('activate')}
                  </button>
                )}
                
                <button onClick={() => handleDelete(plan.id)}>
                  {commonTranslate('delete')}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

## Best Practices

### 1. Use Descriptive Variable Names

```tsx
// ✅ Good - Clear and descriptive
const translate = useTranslate('common');
const commonTranslate = useTranslate('common');
const authTranslate = useTranslate('auth.login');

// ❌ Avoid - Too generic
const t = useTranslate('common');
```

### 2. Use Namespace Helper for Type Safety

```tsx
// ✅ Good - Type-safe with autocomplete
import { TranslationNamespace } from '@/lib/i18n/translate';
const translate = useTranslate(TranslationNamespace.auth.login);

// ❌ Avoid - Prone to typos
const translate = useTranslate('auth.login');
```

### 3. Pass Translation Function to Utilities

```tsx
// ✅ Good - Utilities can use translations
const translate = useTranslate();
await handleAsyncOperation(operation, { translate });

// ❌ Avoid - Utilities can't translate errors
await handleAsyncOperation(operation);
```

### 4. Organize Translations by Feature

```tsx
// ✅ Good - Organized by feature
const commonTranslate = useTranslate('common');
const planTranslate = useTranslate('admin.plans');
const validationTranslate = useTranslate('validation');

// ❌ Avoid - Everything in one namespace
const translate = useTranslate();
```

## Migration from `t()` to `translate()`

If you have existing code using `t()`, you can easily migrate:

```tsx
// Before
import { useTranslations } from 'next-intl';
const t = useTranslations('common');
return <button>{t('save')}</button>;

// After
import { useTranslate } from '@/lib/i18n/translate';
const translate = useTranslate('common');
return <button>{translate('save')}</button>;
```

The functionality is identical - only the name is more descriptive!

