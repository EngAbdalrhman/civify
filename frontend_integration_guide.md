# Frontend Integration Guide - RBAC & Permissions

## Overview

This guide helps frontend developers integrate with the new **Group-Based Permission System** (RBAC) introduced in recent backend commits.

---

## 1. Authentication & User Data

### **Updated User Response** (from `/api/v1/users/me`)

```typescript
interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  permissions: string[];  // ✅ NEW: Replaces "role"
  group: {
    id: number;
    name: string;
    isSystemGroup: boolean;
  };
  subscription: {
    planName: string;
    status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
    expiresAt: string;
    currentCvCount: number;
    cvLimit: number | null;
    currentTokenUsage: number;
    tokenLimit: number | null;
  };
}
```

**Example Response**:
```json
{
  "id": 123,
  "email": "user@example.com",
  "permissions": ["CV.CREATE", "CV.EXPORT.PDF", "API.ACCESS"],
  "group": {
    "name": "Premium User"
  },
  "subscription": {
    "planName": "Premium",
    "status": "ACTIVE",
    "cvLimit": 50,
    "tokenLimit": 100000
  }
}
```

---

## 2. Permission-Based UI Rendering

### **React Hook**

```typescript
// hooks/usePermissions.ts
import { useAuth } from './useAuth';

export const usePermissions = () => {
  const { user } = useAuth();
  
  const hasPermission = (permission: string): boolean => {
    return user?.permissions?.includes(permission) ?? false;
  };
  
  const hasAnyPermission = (permissions: string[]): boolean => {
    return permissions.some(p => hasPermission(p));
  };
  
  const hasAllPermissions = (permissions: string[]): boolean => {
    return permissions.every(p => hasPermission(p));
  };
  
  return { hasPermission, hasAnyPermission, hasAllPermissions };
};
```

### **Permission Guard Component**

```typescript
// components/PermissionGuard.tsx
import React from 'react';
import { usePermissions } from '../hooks/usePermissions';
import { Navigate } from 'react-router-dom';

interface PermissionGuardProps {
  permission: string;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export const PermissionGuard: React.FC<PermissionGuardProps> = ({
  permission,
  children,
  fallback = null
}) => {
  const { hasPermission } = usePermissions();
  
  if (!hasPermission(permission)) {
    return <>{fallback}</>;
  }
  
  return <>{children}</>;
};

// Route Protection
export const ProtectedRoute: React.FC<PermissionGuardProps> = ({
  permission,
  children
}) => {
  const { hasPermission } = usePermissions();
  
  if (!hasPermission(permission)) {
    return <Navigate to="/upgrade" replace />;
  }
  
  return <>{children}</>;
};
```

### **Usage Examples**

```typescript
// Example 1: Conditionally render export button
import { PermissionGuard } from './components/PermissionGuard';

function CVEditor() {
  return (
    <div>
      <button onClick={saveCv}>Save</button>
      
      <PermissionGuard permission="CV.EXPORT.PDF">
        <button onClick={exportPDF}>Export PDF</button>
      </PermissionGuard>
      
      <PermissionGuard 
        permission="CV.NO_WATERMARK"
        fallback={<PremiumBadge />}
      >
        <Checkbox label="Remove Watermark" />
      </PermissionGuard>
    </div>
  );
}

// Example 2: Disable features with tooltips
function CVActions() {
  const { hasPermission } = usePermissions();
  const canExport = hasPermission('CV.EXPORT.PDF');
  
  return (
    <Tooltip title={!canExport ? "Upgrade to export" : ""}>
      <span>
        <button disabled={!canExport} onClick={exportPDF}>
          Export
        </button>
      </span>
    </Tooltip>
  );
}

// Example 3: Route protection
import { ProtectedRoute } from './components/PermissionGuard';

function App() {
  return (
    <Routes>
      <Route path="/dashboard" element={<Dashboard />} />
      
      <Route 
        path="/admin/users" 
        element={
          <ProtectedRoute permission="USER.VIEW">
            <AdminUsersPage />
          </ProtectedRoute>
        } 
      />
    </Routes>
  );
}
```

---

## 3. Error Handling & i18n

### **Error Translation System**

```typescript
// utils/errorTranslation.ts
const ERROR_CODES = {
  PERMISSION_DENIED: {
    en: 'You do not have permission to access this resource',
    ar: 'ليس لديك إذن للوصول إلى هذا المورد'
  },
  TOKEN_LIMIT_EXCEEDED: {
    en: 'Monthly token allowance exceeded. Please upgrade your plan.',
    ar: 'تم تجاوز الحد الشهري للرموز. يرجى ترقية خطتك.'
  },
  CV_LIMIT_REACHED: {
    en: 'You have reached your CV creation limit',
    ar: 'لقد وصلت إلى حد إنشاء السيرة الذاتية'
  },
  SUBSCRIPTION_EXPIRED: {
    en: 'Your subscription has expired',
    ar: 'انتهت صلاحية اشتراكك'
  },
  WEBHOOK_SIGNATURE_INVALID: {
    en: 'Payment verification failed',
    ar: 'فشل التحقق من الدفع'
  }
};

export const translateError = (
  errorCode: string, 
  locale: string = 'en'
): string => {
  const fallbackLocale = 'en';
  const translation = ERROR_CODES[errorCode];
  
  if (!translation) return errorCode;
  
  return translation[locale] || translation[fallbackLocale] || errorCode;
};
```

### **Axios Interceptor**

```typescript
// api/client.ts
import axios from 'axios';
import { translateError } from '../utils/errorTranslation';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor (add auth token)
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor (translate errors)
api.interceptors.response.use(
  response => response,
  error => {
    const locale = localStorage.getItem('locale') || 'en';
    const errorCode = error.response?.data?.code;
    
    if (errorCode) {
      error.translatedMessage = translateError(errorCode, locale);
    }
    
    // Show toast notification
    if (error.translatedMessage) {
      toast.error(error.translatedMessage);
    }
    
    return Promise.reject(error);
  }
);

export default api;
```

---

## 4. WebSocket Notification Integration

### **Notification Service**

```typescript
// services/notificationService.ts
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export interface Notification {
  userId?: number;
  title: string;
  message: string;
  type: 'INFO' | 'WARNING' | 'ERROR' | 'PROMO';
  timestamp: string;
}

class NotificationService {
  private stompClient: Client | null = null;
  private onNotificationCallback: ((notification: Notification) => void) | null = null;
  
  connect(userId: string) {
    const socket = new SockJS(`${process.env.REACT_APP_API_URL}/ws/notifications`);
    
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: str => console.log('[WebSocket]', str),
      
      onConnect: () => {
        console.log('✅ Connected to notification service');
        
        // Subscribe to user-specific notifications
        this.stompClient!.subscribe(
          `/user/${userId}/queue/notifications`, 
          message => {
            const notification: Notification = JSON.parse(message.body);
            this.handleNotification(notification);
          }
        );
        
        // Subscribe to broadcast notifications
        this.stompClient!.subscribe('/topic/notifications', message => {
          const notification: Notification = JSON.parse(message.body);
          this.handleNotification(notification);
        });
      },
      
      onStompError: frame => {
        console.error('❌ STOMP error:', frame);
      },
      
      onDisconnect: () => {
        console.log('⚠️ Disconnected from notification service');
      }
    });
    
    this.stompClient.activate();
  }
  
  private handleNotification(notification: Notification) {
    console.log('🔔 New notification:', notification);
    
    // Show browser notification if permitted
    if (Notification.permission === 'granted') {
      new Notification(notification.title, {
        body: notification.message,
        icon: '/logo.png'
      });
    }
    
    // Trigger callback
    if (this.onNotificationCallback) {
      this.onNotificationCallback(notification);
    }
  }
  
  onNotification(callback: (notification: Notification) => void) {
    this.onNotificationCallback = callback;
  }
  
  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }
}

export default new NotificationService();
```

### **React Integration**

```typescript
// hooks/useNotifications.ts
import { useEffect } from 'react';
import { useAuth } from './useAuth';
import notificationService, { Notification } from '../services/notificationService';
import { toast } from 'react-toastify';

export const useNotifications = () => {
  const { user } = useAuth();
  
  useEffect(() => {
    if (!user) return;
    
    // Connect to WebSocket
    notificationService.connect(user.id.toString());
    
    // Handle incoming notifications
    notificationService.onNotification((notification: Notification) => {
      // Show toast based on type
      switch (notification.type) {
        case 'INFO':
          toast.info(notification.message);
          break;
        case 'WARNING':
          toast.warning(notification.message);
          break;
        case 'ERROR':
          toast.error(notification.message);
          break;
        case 'PROMO':
          toast.success(notification.message, { autoClose: 10000 });
          break;
      }
    });
    
    // Cleanup on unmount
    return () => {
      notificationService.disconnect();
    };
  }, [user]);
};

// Usage in App.tsx
function App() {
  useNotifications(); // ✅ Auto-connects and shows toasts
  
  return <div>...</div>;
}
```

---

## 5. Subscription Status Handling

### **Subscription Hook**

```typescript
// hooks/useSubscription.ts
import { useAuth } from './useAuth';

export const useSubscription = () => {
  const { user } = useAuth();
  const subscription = user?.subscription;
  
  const isActive = subscription?.status === 'ACTIVE';
  const isExpired = subscription?.status === 'EXPIRED';
  
  const cvLimitReached = subscription?.cvLimit !== null && 
    subscription?.currentCvCount >= subscription?.cvLimit;
  
  const tokenLimitReached = subscription?.tokenLimit !== null &&
    subscription?.currentTokenUsage >= subscription?.tokenLimit;
  
  const canCreateCv = isActive && !cvLimitReached;
  const canUseTokens = isActive && !tokenLimitReached;
  
  return {
    subscription,
    isActive,
    isExpired,
    cvLimitReached,
    tokenLimitReached,
    canCreateCv,
    canUseTokens
  };
};
```

### **Usage Examples**

```typescript
function CVDashboard() {
  const { subscription, canCreateCv, cvLimitReached } = useSubscription();
  
  return (
    <div>
      {cvLimitReached && (
        <Alert severity="warning">
          You've reached your CV limit ({subscription.currentCvCount}/{subscription.cvLimit}). 
          <Link to="/upgrade">Upgrade now</Link>
        </Alert>
      )}
      
      <button 
        disabled={!canCreateCv}
        onClick={createNewCv}
      >
        Create New CV
      </button>
    </div>
  );
}
```

---

## 6. API Endpoints Reference

### **User & Permissions**
- `GET /api/v1/users/me` - Get current user with permissions
- `GET /api/v1/subscription/my-subscription` - Get active subscription

### **Admin (requires permissions)**
- `GET /api/v1/admin/users` - `USER.VIEW`
- `POST /api/v1/admin/groups` - `GROUP.MODIFY`
- `GET /api/v1/admin/subscriptions` - `SUBSCRIPTION.VIEW`

### **Subscription**
- `POST /api/v1/payments/initiate` - Initiate payment
- `GET /api/v1/subscription/can-use/CV` - Check if can create CV

---

## 7. Migration Checklist for Frontend

- [ ] Remove all references to `user.role`
- [ ] Update to use `user.permissions` array
- [ ] Implement `usePermissions` hook
- [ ] Replace role checks with permission checks
- [ ] Add `PermissionGuard` components
- [ ] Implement error translation system
- [ ] Connect to WebSocket notifications
- [ ] Test permission-based UI hiding/showing
- [ ] Test subscription limit warnings
- [ ] Update TypeScript interfaces

---

## 8. Common Permissions

| Permission | Description |
|------------|-------------|
| `CV.CREATE` | Create and edit CVs |
| `CV.EXPORT.PDF` | Export CV as PDF |
| `CV.NO_WATERMARK` | Remove watermark from exports |
| `API.ACCESS` | Access developer API |
| `USER.VIEW` | View users (admin) |
| `PLAN.MODIFY` | Manage plans (admin) |
| `GROUP.MODIFY` | Manage groups (admin) |

---

## Summary

✅ **Replace `role` with `permissions`**  
✅ **Use `PermissionGuard` for conditional rendering**  
✅ **Implement error translation with locale fallback**  
✅ **Connect to WebSocket for real-time notifications**  
✅ **Monitor subscription status and limits**

**Next Steps**: Apply changes to your frontend codebase and test with the updated backend.
