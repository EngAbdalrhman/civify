# Internationalization (i18n) Usage Guide

## Quick Start

### 1. Using Translations in Components

#### Client Components

```tsx
"use client";

import { useTranslate } from "@/lib/i18n/translate";

export function MyComponent() {
  const translate = useTranslate("common");

  return (
    <div>
      <h1>{translate("title")}</h1>
      <button>{translate("save")}</button>
      <button>{translate("cancel")}</button>
    </div>
  );
}
```

#### Server Components

```tsx
import { getTranslate } from "@/lib/i18n/translate";

export default async function MyPage() {
  const translate = await getTranslate("common");

  return (
    <div>
      <h1>{translate("title")}</h1>
    </div>
  );
}
```

**Note:** We use `translate()` instead of `t()` for better code readability and clarity.

### 2. Using Nested Translation Keys

```tsx
import { useTranslate } from '@/lib/i18n/translate';

const translate = useTranslate('auth.login');

// Translates to "Login" in English, "تسجيل الدخول" in Arabic
<h1>{translate('title')}</h1>

// Translates to "Email" in English, "البريد الإلكتروني" in Arabic
<label>{translate('email')}</label>
```

### 3. Using Translations with Parameters

```tsx
import { useTranslate } from "@/lib/i18n/translate";

const translate = useTranslate("validation");

// English: "Name is required"
// Arabic: "الاسم مطلوب"
const error = translate("required", { field: "Name" });

// English: "Password must be at least 8 characters"
// Arabic: "كلمة المرور يجب أن تكون على الأقل 8"
const error2 = translate("min", { field: "Password", min: 8 });
```

### 4. Adding the Language Switcher

Add the `LanguageSwitcher` component to your navigation or header:

```tsx
import { LanguageSwitcher } from "@/components/LanguageSwitcher";

export function Header() {
  return (
    <header>
      <nav>
        {/* Your navigation items */}
        <LanguageSwitcher />
      </nav>
    </header>
  );
}
```

### 5. Using Error Handling with Translations

```tsx
"use client";

import { useTranslations } from "next-intl";
import { handleAsyncOperation, showSuccessToast } from "@/lib/error-handler";
import { apiClient } from "@/lib/api/client";

export function MyForm() {
  const t = useTranslations();

  const handleSubmit = async (data: any) => {
    const result = await handleAsyncOperation(
      () => apiClient.post("/endpoint", data),
      {
        successMessage: t("common.created"),
        errorTitle: t("common.error"),
        t,
      }
    );

    if (result) {
      // Handle success
    }
  };

  return <form onSubmit={handleSubmit}>{/* Form fields */}</form>;
}
```

### 6. Form Validation with Translations

```tsx
import { validateField } from "@/lib/error-handler";
import { useTranslations } from "next-intl";

export function MyForm() {
  const t = useTranslations();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleValidation = (name: string, value: string) => {
    const error = validateField(
      value,
      { required: true, min: 3, max: 50 },
      t("admin.plans.planName"),
      t
    );

    setErrors((prev) => ({
      ...prev,
      [name]: error || "",
    }));
  };

  return (
    <div>
      <input onChange={(e) => handleValidation("name", e.target.value)} />
      {errors.name && <span className="text-red-500">{errors.name}</span>}
    </div>
  );
}
```

## Translation File Structure

### English (`src/i18n/messages/en.json`)

```json
{
  "common": {
    "save": "Save",
    "cancel": "Cancel",
    "delete": "Delete"
  },
  "auth": {
    "login": {
      "title": "Login",
      "email": "Email",
      "password": "Password"
    }
  }
}
```

### Arabic (`src/i18n/messages/ar.json`)

```json
{
  "common": {
    "save": "حفظ",
    "cancel": "إلغاء",
    "delete": "حذف"
  },
  "auth": {
    "login": {
      "title": "تسجيل الدخول",
      "email": "البريد الإلكتروني",
      "password": "كلمة المرور"
    }
  }
}
```

## Adding New Translations

### Step 1: Add to English file

```json
// src/i18n/messages/en.json
{
  "myFeature": {
    "title": "My Feature",
    "description": "This is my new feature",
    "action": "Click here"
  }
}
```

### Step 2: Add to Arabic file

```json
// src/i18n/messages/ar.json
{
  "myFeature": {
    "title": "ميزتي",
    "description": "هذه ميزتي الجديدة",
    "action": "انقر هنا"
  }
}
```

### Step 3: Use in component

```tsx
const t = useTranslations("myFeature");

<div>
  <h1>{t("title")}</h1>
  <p>{t("description")}</p>
  <button>{t("action")}</button>
</div>;
```

## RTL Support

The layout automatically switches to RTL when Arabic is selected. No additional code needed!

### Checking if RTL is active

```tsx
import { useLocale } from "next-intl";

export function MyComponent() {
  const locale = useLocale();
  const isRtl = locale === "ar";

  return <div className={isRtl ? "text-right" : "text-left"}>Content</div>;
}
```

## API Integration

The API client automatically sends the `Accept-Language` header with every request:

```tsx
// No need to manually add Accept-Language header
const response = await apiClient.get("/plans");
// Header is automatically added: Accept-Language: en (or ar)
```

## Backend Integration

The backend automatically detects the locale from the `Accept-Language` header and returns localized error messages:

```java
// In your service or controller
@Autowired
private MessageService messageService;

public void myMethod() {
    // Get localized message for current request locale
    String message = messageService.getMessage("subscription.plan.created");

    // With parameters
    String message2 = messageService.getMessage(
        "validation.required",
        new Object[]{"Plan Name"}
    );
}
```

## Common Patterns

### 1. Conditional Messages

```tsx
const t = useTranslations("subscription");

const message = isActive ? t("plan.activated") : t("plan.deactivated");
```

### 2. Dynamic Keys

```tsx
const t = useTranslations("common");

const status = "success"; // or 'error', 'warning'
const message = t(status);
```

### 3. Pluralization

```tsx
const t = useTranslations("subscription");

const count = 5;
const message = count === 1 ? t("plan.singular") : t("plan.plural", { count });
```

### 4. Date/Time Formatting

```tsx
import { useFormatter } from "next-intl";

export function MyComponent() {
  const format = useFormatter();

  const date = new Date();
  const formattedDate = format.dateTime(date, {
    year: "numeric",
    month: "long",
    day: "numeric",
  });

  return <span>{formattedDate}</span>;
}
```

### 5. Number Formatting

```tsx
import { useFormatter } from "next-intl";

export function PriceDisplay({ price }: { price: number }) {
  const format = useFormatter();

  const formattedPrice = format.number(price, {
    style: "currency",
    currency: "USD",
  });

  return <span>{formattedPrice}</span>;
}
```

## Best Practices

1. **Always use translation keys** - Never hardcode text in components
2. **Organize by feature** - Group related translations under the same namespace
3. **Keep keys consistent** - Use the same structure in both language files
4. **Use descriptive keys** - Make keys self-documenting (e.g., `auth.login.title` not `a.l.t`)
5. **Test both languages** - Always verify translations in both English and Arabic
6. **Check RTL layout** - Ensure UI looks correct in RTL mode
7. **Use error handler** - Always use the centralized error handler for consistent error messages

## Troubleshooting

### Translations not showing

1. Check that the key exists in both `en.json` and `ar.json`
2. Verify the namespace is correct (e.g., `useTranslations('common')`)
3. Check browser console for errors

### RTL not working

1. Verify the `dir` attribute is set on the `<html>` tag
2. Check that Tailwind CSS is configured for RTL
3. Clear browser cache and reload

### Language not switching

1. Check that the middleware is configured correctly
2. Verify the locale is being set in the URL or cookie
3. Check browser console for errors

### API not receiving locale

1. Verify the `Accept-Language` header is being sent (check Network tab)
2. Check that the API client is properly configured
3. Ensure the backend is reading the header correctly

## Examples

See the following files for complete examples:

- `src/app/admin/plans/page.tsx` - Full admin page with translations
- `src/components/LanguageSwitcher.tsx` - Language switcher component
- `src/lib/error-handler.ts` - Error handling with translations
