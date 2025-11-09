# Theme System Guide

## Overview

HICv includes a comprehensive theme system with three built-in themes:
- **Light** - Modern and clean light theme
- **Dark** - Modern dark theme
- **Vintage** - Warm and classic theme with sepia tones

The theme system is built on CSS variables, making it easy to customize colors and add new themes.

## Features

✅ **Three Built-in Themes** - Light, Dark, and Vintage
✅ **Persistent Theme Selection** - Theme preference saved to localStorage
✅ **Easy Theme Switching** - Dropdown menu with theme selector
✅ **CSS Variables** - Easy to customize and extend
✅ **Type-Safe** - TypeScript support for theme values
✅ **Localized** - Theme names translated to Arabic and English
✅ **No Flash** - Prevents flash of unstyled content on page load

## Usage

### Basic Theme Switcher

Use the standalone `ThemeSwitcher` component:

```tsx
import { ThemeSwitcher } from "@/components/ThemeSwitcher";

export function Header() {
  return (
    <header>
      <nav>
        <ThemeSwitcher />
      </nav>
    </header>
  );
}
```

### Combined Settings Menu

Use the `SettingsMenu` component for both language and theme switching:

```tsx
import { SettingsMenu } from "@/components/SettingsMenu";

export function Header() {
  return (
    <header>
      <nav>
        <SettingsMenu />
      </nav>
    </header>
  );
}
```

### Using Theme in Components

Access the current theme in your components:

```tsx
"use client";

import { useTheme } from "@/lib/theme/theme-provider";

export function MyComponent() {
  const { theme, setTheme } = useTheme();
  
  return (
    <div>
      <p>Current theme: {theme}</p>
      <button onClick={() => setTheme("dark")}>Switch to Dark</button>
    </div>
  );
}
```

## Theme Colors

### Light Theme

Modern and clean with vibrant accent colors:

```css
--background: 0 0% 100%           /* Pure white */
--foreground: 222.2 84% 4.9%      /* Dark blue-gray */
--primary: 243 77% 62%            /* Vibrant blue */
--accent: 256 91% 76%             /* Purple accent */
```

### Dark Theme

Modern dark with high contrast:

```css
--background: 222.2 84% 4.9%      /* Dark blue-gray */
--foreground: 210 40% 98%         /* Off-white */
--primary: 243 77% 62%            /* Vibrant blue */
--accent: 256 91% 76%             /* Purple accent */
```

### Vintage Theme

Warm and classic with sepia tones:

```css
--background: 40 40% 96%          /* Warm off-white */
--foreground: 30 20% 15%          /* Dark brown */
--primary: 25 75% 47%             /* Burnt orange */
--accent: 35 65% 55%              /* Warm amber */
```

## Customizing Themes

### Modifying Existing Themes

Edit `src/app/globals.css` to customize theme colors:

```css
@layer base {
  .light {
    --primary: 200 100% 50%;  /* Change primary color */
    --accent: 150 80% 60%;    /* Change accent color */
  }
}
```

### Adding a New Theme

1. **Add theme colors to `globals.css`:**

```css
@layer base {
  .ocean {
    --background: 200 20% 98%;
    --foreground: 200 50% 10%;
    --primary: 200 80% 50%;
    --accent: 180 70% 60%;
    /* ... add all other CSS variables */
  }
}
```

2. **Update the theme type in `theme-provider.tsx`:**

```typescript
type Theme = "light" | "dark" | "vintage" | "ocean";
```

3. **Add theme to the switcher in `ThemeSwitcher.tsx`:**

```typescript
const themes = [
  { value: "light" as const, icon: Sun, labelKey: "theme.light" },
  { value: "dark" as const, icon: Moon, labelKey: "theme.dark" },
  { value: "vintage" as const, icon: Clock, labelKey: "theme.vintage" },
  { value: "ocean" as const, icon: Waves, labelKey: "theme.ocean" },
];
```

4. **Add translations to `en.json` and `ar.json`:**

```json
{
  "theme": {
    "ocean": "Ocean"
  }
}
```

## CSS Variables Reference

All themes must define these CSS variables:

### Core Colors
- `--background` - Main background color
- `--foreground` - Main text color
- `--card` - Card background
- `--card-foreground` - Card text color
- `--popover` - Popover background
- `--popover-foreground` - Popover text color

### Brand Colors
- `--primary` - Primary brand color
- `--primary-foreground` - Text on primary color
- `--secondary` - Secondary color
- `--secondary-foreground` - Text on secondary color
- `--accent` - Accent color
- `--accent-foreground` - Text on accent color

### UI Colors
- `--muted` - Muted background
- `--muted-foreground` - Muted text
- `--destructive` - Error/danger color
- `--destructive-foreground` - Text on destructive color
- `--border` - Border color
- `--input` - Input border color
- `--ring` - Focus ring color

### Chart Colors
- `--chart-1` through `--chart-5` - Chart colors

### Sidebar Colors
- `--sidebar-background` - Sidebar background
- `--sidebar-foreground` - Sidebar text
- `--sidebar-primary` - Sidebar primary color
- `--sidebar-primary-foreground` - Sidebar primary text
- `--sidebar-accent` - Sidebar accent
- `--sidebar-accent-foreground` - Sidebar accent text
- `--sidebar-border` - Sidebar border
- `--sidebar-ring` - Sidebar focus ring

### Other
- `--radius` - Border radius (e.g., "0.5rem")

## Using Theme Colors in Components

### With Tailwind Classes

```tsx
<div className="bg-background text-foreground">
  <button className="bg-primary text-primary-foreground">
    Primary Button
  </button>
  <button className="bg-accent text-accent-foreground">
    Accent Button
  </button>
</div>
```

### With Custom CSS

```css
.my-component {
  background-color: hsl(var(--background));
  color: hsl(var(--foreground));
  border: 1px solid hsl(var(--border));
}
```

## Theme-Specific Styles

Apply styles only for specific themes:

```tsx
<div className="bg-white dark:bg-gray-900 vintage:bg-amber-50">
  Theme-specific background
</div>
```

Or in CSS:

```css
.my-component {
  background: white;
}

.dark .my-component {
  background: #1a1a1a;
}

.vintage .my-component {
  background: #fef3e2;
}
```

## Best Practices

### 1. Always Use CSS Variables

```tsx
// ✅ Good - Uses theme colors
<div className="bg-background text-foreground">

// ❌ Avoid - Hardcoded colors
<div className="bg-white text-black">
```

### 2. Use Semantic Color Names

```tsx
// ✅ Good - Semantic
<button className="bg-primary text-primary-foreground">

// ❌ Avoid - Specific colors
<button className="bg-blue-500 text-white">
```

### 3. Test All Themes

Always test your components in all three themes to ensure proper contrast and readability.

### 4. Provide Theme-Specific Overrides When Needed

```tsx
<div className="bg-card dark:shadow-lg vintage:border-2">
  Content
</div>
```

## Components

### ThemeProvider

Wraps the application and manages theme state:

```tsx
<ThemeProvider defaultTheme="light" storageKey="hicv-theme">
  {children}
</ThemeProvider>
```

**Props:**
- `defaultTheme` - Initial theme (default: "light")
- `storageKey` - localStorage key (default: "hicv-theme")

### ThemeSwitcher

Standalone theme switcher button with dropdown:

```tsx
<ThemeSwitcher />
```

### SettingsMenu

Combined settings menu with language and theme options:

```tsx
<SettingsMenu />
```

### useTheme Hook

Access theme state in components:

```tsx
const { theme, setTheme } = useTheme();
```

**Returns:**
- `theme` - Current theme ("light" | "dark" | "vintage")
- `setTheme` - Function to change theme

## Troubleshooting

### Theme Not Persisting

Make sure `ThemeProvider` is wrapping your app in the root layout.

### Flash of Unstyled Content

The theme provider prevents this by default. If you still see it, ensure:
1. `suppressHydrationWarning={true}` is set on the `<body>` tag
2. Theme is loaded before rendering

### Colors Not Updating

1. Check that you're using CSS variables: `hsl(var(--color-name))`
2. Verify the theme class is applied to the `<html>` element
3. Clear browser cache and localStorage

### Custom Colors Not Working

Make sure you're using the HSL format without the `hsl()` wrapper:

```css
/* ✅ Correct */
--primary: 243 77% 62%;

/* ❌ Wrong */
--primary: hsl(243, 77%, 62%);
```

## Summary

The HICv theme system provides:
- ✅ Three beautiful, production-ready themes
- ✅ Easy customization through CSS variables
- ✅ Type-safe theme management
- ✅ Persistent user preferences
- ✅ Localized theme names
- ✅ Simple API for theme switching
- ✅ No flash of unstyled content

All themes are fully accessible and tested for proper contrast ratios.

