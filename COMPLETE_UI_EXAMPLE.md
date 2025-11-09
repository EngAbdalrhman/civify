# Complete UI Example

This document shows a complete example of using all UI enhancements together.

## Example: Admin Dashboard with All Features

```tsx
"use client";

import { useState } from "react";
import { useTranslate } from "@/lib/i18n/translate";
import { useTheme } from "@/lib/theme/theme-provider";
import { SettingsMenu } from "@/components/SettingsMenu";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { ThemeSwitcher } from "@/components/ThemeSwitcher";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { handleAsyncOperation, showSuccessToast, showErrorToast } from "@/lib/error-handler";
import { apiClient } from "@/lib/api/client";

export default function AdminDashboard() {
  const translate = useTranslate();
  const { theme } = useTheme();
  const [loading, setLoading] = useState(false);

  // Example: Create a new plan with error handling
  const handleCreatePlan = async () => {
    setLoading(true);
    
    const result = await handleAsyncOperation(
      () => apiClient.post("/admin/plans", {
        name: "Premium Plan",
        price: 99.99,
        features: ["Feature 1", "Feature 2"],
      }),
      {
        successMessage: translate("admin.plans.created"),
        errorTitle: translate("common.error"),
        translate,
        onSuccess: (data) => {
          console.log("Plan created:", data);
          // Refresh plans list or navigate
        },
      }
    );
    
    setLoading(false);
  };

  // Example: Manual error handling
  const handleManualError = () => {
    try {
      throw new Error("Something went wrong!");
    } catch (error) {
      showErrorToast(
        error,
        translate,
        { title: translate("common.error") }
      );
    }
  };

  // Example: Manual success toast
  const handleManualSuccess = () => {
    showSuccessToast(
      translate("common.updated"),
      { title: translate("common.success") }
    );
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Header with Settings Menu */}
      <header className="border-b bg-card">
        <div className="container flex items-center justify-between h-16 px-4">
          <div className="flex items-center gap-4">
            <h1 className="text-2xl font-bold text-foreground">
              {translate("admin.dashboard.title")}
            </h1>
            <span className="text-sm text-muted-foreground">
              {translate("theme.title")}: {translate(`theme.${theme}`)}
            </span>
          </div>
          
          {/* Combined Settings Menu */}
          <div className="flex items-center gap-2">
            <SettingsMenu />
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container py-8 px-4">
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          
          {/* Card 1: Theme Demo */}
          <Card>
            <CardHeader>
              <CardTitle>{translate("theme.title")}</CardTitle>
              <CardDescription>
                {translate("theme.switchTo", { theme: translate(`theme.${theme}`) })}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="p-4 bg-primary text-primary-foreground rounded">
                  Primary Color
                </div>
                <div className="p-4 bg-accent text-accent-foreground rounded">
                  Accent Color
                </div>
                <div className="p-4 bg-muted text-muted-foreground rounded">
                  Muted Color
                </div>
                <ThemeSwitcher />
              </div>
            </CardContent>
          </Card>

          {/* Card 2: Language Demo */}
          <Card>
            <CardHeader>
              <CardTitle>{translate("language.title")}</CardTitle>
              <CardDescription>
                {translate("common.info")}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <p className="text-sm text-muted-foreground">
                  {translate("common.loading")}
                </p>
                <p className="text-sm text-muted-foreground">
                  {translate("common.save")} | {translate("common.cancel")}
                </p>
                <LanguageSwitcher />
              </div>
            </CardContent>
          </Card>

          {/* Card 3: Error Handling Demo */}
          <Card>
            <CardHeader>
              <CardTitle>{translate("common.error")}</CardTitle>
              <CardDescription>
                Toast Notifications Demo
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Button
                  onClick={handleCreatePlan}
                  disabled={loading}
                  className="w-full"
                >
                  {loading ? translate("common.loading") : translate("admin.plans.create")}
                </Button>
                
                <Button
                  onClick={handleManualSuccess}
                  variant="outline"
                  className="w-full"
                >
                  Show Success Toast
                </Button>
                
                <Button
                  onClick={handleManualError}
                  variant="destructive"
                  className="w-full"
                >
                  Show Error Toast
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Card 4: All Components Together */}
          <Card className="md:col-span-2 lg:col-span-3">
            <CardHeader>
              <CardTitle>Complete Example</CardTitle>
              <CardDescription>
                All UI enhancements working together
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 md:grid-cols-3">
                <div className="space-y-2">
                  <h3 className="font-semibold text-primary">
                    {translate("theme.title")}
                  </h3>
                  <ul className="text-sm text-muted-foreground space-y-1">
                    <li>✓ {translate("theme.light")}</li>
                    <li>✓ {translate("theme.dark")}</li>
                    <li>✓ {translate("theme.vintage")}</li>
                  </ul>
                </div>
                
                <div className="space-y-2">
                  <h3 className="font-semibold text-primary">
                    {translate("language.title")}
                  </h3>
                  <ul className="text-sm text-muted-foreground space-y-1">
                    <li>✓ {translate("language.english")}</li>
                    <li>✓ {translate("language.arabic")}</li>
                    <li>✓ RTL Support</li>
                  </ul>
                </div>
                
                <div className="space-y-2">
                  <h3 className="font-semibold text-primary">
                    Error Handling
                  </h3>
                  <ul className="text-sm text-muted-foreground space-y-1">
                    <li>✓ Toast Notifications</li>
                    <li>✓ Localized Messages</li>
                    <li>✓ Auto Error Handling</li>
                  </ul>
                </div>
              </div>
            </CardContent>
          </Card>

        </div>
      </main>
    </div>
  );
}
```

## Example: Simple Page with Settings

```tsx
"use client";

import { SettingsMenu } from "@/components/SettingsMenu";
import { useTranslate } from "@/lib/i18n/translate";

export default function SimplePage() {
  const translate = useTranslate();

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b">
        <div className="container flex justify-between items-center h-16">
          <h1 className="text-xl font-bold">{translate("app.name")}</h1>
          <SettingsMenu />
        </div>
      </header>
      
      <main className="container py-8">
        <h2 className="text-2xl font-bold mb-4">
          {translate("common.info")}
        </h2>
        <p className="text-muted-foreground">
          {translate("app.description")}
        </p>
      </main>
    </div>
  );
}
```

## Example: Form with Validation and Error Handling

```tsx
"use client";

import { useState } from "react";
import { useTranslate } from "@/lib/i18n/translate";
import { validateField, handleAsyncOperation } from "@/lib/error-handler";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { apiClient } from "@/lib/api/client";

export function CreatePlanForm() {
  const translate = useTranslate();
  const [formData, setFormData] = useState({
    name: "",
    price: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const handleValidation = (field: string, value: string) => {
    let error: string | null = null;

    if (field === "name") {
      error = validateField(
        value,
        { required: true, min: 3, max: 50 },
        translate("admin.plans.planName"),
        translate
      );
    } else if (field === "price") {
      error = validateField(
        value,
        { required: true },
        translate("admin.plans.price"),
        translate
      );
    }

    setErrors((prev) => ({
      ...prev,
      [field]: error || "",
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validate all fields
    handleValidation("name", formData.name);
    handleValidation("price", formData.price);

    // Check if there are any errors
    if (Object.values(errors).some((error) => error)) {
      return;
    }

    setLoading(true);

    await handleAsyncOperation(
      () =>
        apiClient.post("/admin/plans", {
          name: formData.name,
          price: parseFloat(formData.price),
        }),
      {
        successMessage: translate("admin.plans.created"),
        errorTitle: translate("common.error"),
        translate,
        onSuccess: () => {
          // Reset form
          setFormData({ name: "", price: "" });
          setErrors({});
        },
      }
    );

    setLoading(false);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <Label htmlFor="name">{translate("admin.plans.planName")}</Label>
        <Input
          id="name"
          value={formData.name}
          onChange={(e) => {
            setFormData({ ...formData, name: e.target.value });
            handleValidation("name", e.target.value);
          }}
          className={errors.name ? "border-destructive" : ""}
        />
        {errors.name && (
          <p className="text-sm text-destructive mt-1">{errors.name}</p>
        )}
      </div>

      <div>
        <Label htmlFor="price">{translate("admin.plans.price")}</Label>
        <Input
          id="price"
          type="number"
          step="0.01"
          value={formData.price}
          onChange={(e) => {
            setFormData({ ...formData, price: e.target.value });
            handleValidation("price", e.target.value);
          }}
          className={errors.price ? "border-destructive" : ""}
        />
        {errors.price && (
          <p className="text-sm text-destructive mt-1">{errors.price}</p>
        )}
      </div>

      <div className="flex gap-2">
        <Button type="submit" disabled={loading}>
          {loading ? translate("common.loading") : translate("common.create")}
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={() => {
            setFormData({ name: "", price: "" });
            setErrors({});
          }}
        >
          {translate("common.cancel")}
        </Button>
      </div>
    </form>
  );
}
```

## Example: Using Theme-Specific Styles

```tsx
"use client";

import { useTheme } from "@/lib/theme/theme-provider";

export function ThemedComponent() {
  const { theme } = useTheme();

  return (
    <div className="p-6 rounded-lg bg-card border">
      {/* Theme-specific background */}
      <div className="p-4 rounded bg-white dark:bg-gray-900 vintage:bg-amber-50">
        <h2 className="text-2xl font-bold text-foreground">
          Current Theme: {theme}
        </h2>
      </div>

      {/* Theme-specific text colors */}
      <p className="mt-4 text-gray-700 dark:text-gray-300 vintage:text-amber-900">
        This text adapts to the theme
      </p>

      {/* Theme-specific borders */}
      <div className="mt-4 p-4 border-2 border-gray-200 dark:border-gray-700 vintage:border-amber-300">
        Theme-specific border
      </div>

      {/* Using CSS variables (recommended) */}
      <div className="mt-4 p-4 bg-primary text-primary-foreground rounded">
        This uses theme CSS variables
      </div>
    </div>
  );
}
```

## Quick Reference

### Import Statements

```tsx
// Translation
import { useTranslate } from "@/lib/i18n/translate";

// Theme
import { useTheme } from "@/lib/theme/theme-provider";

// Components
import { SettingsMenu } from "@/components/SettingsMenu";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { ThemeSwitcher } from "@/components/ThemeSwitcher";

// Error Handling
import {
  showErrorToast,
  showSuccessToast,
  handleAsyncOperation,
  validateField,
} from "@/lib/error-handler";
```

### Common Patterns

```tsx
// Get translation function
const translate = useTranslate();

// Get current theme
const { theme, setTheme } = useTheme();

// Show success toast
showSuccessToast(translate("common.created"));

// Show error toast
showErrorToast(error, translate);

// Handle async operation
await handleAsyncOperation(
  () => apiClient.post("/endpoint", data),
  {
    successMessage: translate("common.created"),
    translate,
  }
);

// Validate field
const error = validateField(
  value,
  { required: true, min: 3 },
  translate("fieldName"),
  translate
);
```

## Summary

This example demonstrates:
- ✅ Using SettingsMenu for combined language/theme switching
- ✅ Error handling with toast notifications
- ✅ Form validation with localized messages
- ✅ Theme-aware components
- ✅ Localized UI text
- ✅ Async operation handling
- ✅ Manual toast notifications

All features work seamlessly together to provide a polished, production-ready user experience!

