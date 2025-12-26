# API Permissions Documentation

This document describes all permissions used across the Civify API and their associated endpoints.

## Permission Structure

Permissions follow a hierarchical naming convention: `DOMAIN.ACTION`

- **DOMAIN**: The resource or feature area (e.g., `USER`, `GROUP`, `SUBSCRIPTION`)
- **ACTION**: The operation type (e.g., `VIEW`, `MODIFY`, `SEND`)

## Permission Catalog

### 1. User Management (`USER.*`)

| Permission | Description | Endpoints | Access Level |
|:---|:---|:---|:---|
| **USER.VIEW** | View user data, filter users, retrieve user details | `POST /api/v1/admin/users/filter`<br>`GET /api/v1/admin/users`<br>`GET /api/v1/admin/users/{userId}` | Admin |
| **USER.MODIFY** | Update user limits (CV, API quotas) | `PUT /api/v1/admin/users/{userId}/limits` | Admin |

**Explanation**: 
- `USER.VIEW` grants read-only access to user management  
- `USER.MODIFY` allows changing user-specific resource limits

---

### 2.Group & Permissions Management (`GROUP.*`)

| Permission | Description | Endpoints | Access Level |
|:---|:---|:---|:---|
| **GROUP.VIEW** | View permission groups, retrieve group details and permissions | `POST /api/v1/admin/groups/filter`<br>`GET /api/v1/admin/groups/{groupId}`<br>`GET /api/v1/admin/groups/{groupId}/permissions`<br>`GET /api/v1/admin/groups` | Super Admin |
| **GROUP.MODIFY** | Create, update, delete groups; assign/revoke permissions | `POST /api/v1/admin/groups`<br>`PUT /api/v1/admin/groups/{groupId}`<br>`DELETE /api/v1/admin/groups/{groupId}`<br>`POST /api/v1/admin/groups/{groupId}/permissions/{permissionId}` | Super Admin |

**Explanation**:
- `GROUP.VIEW` is for auditing and reviewing permission structures  
- `GROUP.MODIFY` is highly privileged—only for administrators managing RBAC

---

### 3. Subscription & Plan Management (`SUBSCRIPTION.*`)

| Permission | Description | Endpoints | Access Level |
|:---|:---|:---|:---|
| **SUBSCRIPTION.VIEW** | View all subscriptions with filtering | `POST /api/v1/admin/subscriptions/filter` | Admin |
| **PLAN.VIEW** | View, filter, and retrieve plan details | `POST /api/v1/admin/plans/filter`<br>`GET /api/v1/admin/plans/{id}`<br>`GET /api/v1/admin/plans/{id}/stats` | Admin |
| **PLAN.MODIFY** | Create, update, activate, deactivate, and delete plans | `POST /api/v1/admin/plans`<br>`PUT /api/v1/admin/plans/{id}`<br>`POST /api/v1/admin/plans/{id}/activate`<br>`POST /api/v1/admin/plans/{id}/deactivate`<br>`DELETE /api/v1/admin/plans/{id}` | Admin |

**Explanation**:
- `SUBSCRIPTION.VIEW` allows tracking active subscriptions  
- `PLAN.VIEW` is for reviewing available plans  
- `PLAN.MODIFY` controls the entire plan lifecycle

---

### 4. Promo Code Management (`PROMO.*`)

| Permission | Description | Endpoints | Access Level |
|:---|:---|:---|:---|
| **PROMO.VIEW** | View and filter promo codes | `POST /api/v1/admin/promocodes/filter` | Admin |
| **PROMO.MODIFY** | Create, update, deactivate, and delete promo codes | `POST /api/v1/admin/promocodes`<br>`PUT /api/v1/admin/promocodes/{id}`<br>`POST /api/v1/admin/promocodes/{id}/deactivate`<br>`DELETE /api/v1/admin/promocodes/{id}` | Admin |

**Explanation**:
- `PROMO.VIEW` for monitoring active promotions  
- `PROMO.MODIFY` for full campaign management

---

### 5. System Logs & Auditing (`SYSTEM.LOGS.*`)

| Permission | Description | Endpoints | Access Level |
|:---|:---|:---|:---|
| **SYSTEM.LOGS.VIEW** | View all system logs (API usage, audit logs, stats) | `POST /api/v1/logs/admin/filter` (API usage)<br>`POST /api/v1/admin/audit/search` (Audit logs)<br>`GET /api/v1/admin/audit/{entityType}/{entityId}`<br>`GET /api/v1/admin/audit/{entityType}`<br>`GET /api/v1/admin/audit`<br>`GET /api/v1/admin/stats` | Admin / Super Admin |

**Explanation**:
- Grants read access to sensitive operational data  
- Required for compliance auditing and system monitoring

---

### 6. CV & User Features (`CV.*`)

| Permission | Description | Endpoints | Access Level |
|:---|:---|:---|:---|
| **CV.CREATE** | Access CV-related features (subscriptions, API usage logs) | `GET /api/v1/subscriptions/my-subscription`<br>`GET /api/v1/subscriptions/my-subscriptions`<br>`POST /api/v1/subscriptions/cancel/{id}`<br>`POST /api/v1/subscriptions/renew/{id}`<br>`GET /api/v1/subscriptions/can-use/{feature}`<br>`POST /api/v1/logs/my-logs/filter`<br>`GET /api/v1/logs/my-stats` | Authenticated User |

**Explanation**:
- Basic permission for authenticated users to manage their subscriptions and view personal logs
- **Note**: Currently covers upload, tailor, and export operations under a single permission
- **Recommended**: Split into separate permissions (`CV.UPLOAD`, `CV.TAILOR`, `CV.EXPORT`) for finer-grained access control

---

### 7. Notifications (`NOTIF.*`)

| Permission | Description | Endpoints | Access Level |
|:---|:---|:---|:---|
| **NOTIF.SEND** | Send broadcast and user-specific notifications | `POST /api/v1/admin/notifications/broadcast`<br>`POST /api/v1/admin/notifications/user` | Admin |

**Explanation**:
- Controls ability to push system notifications  
- Used for announcements, alerts, and user messaging

---

## Permission Hierarchy

```
┌─────────────────────────────────────┐
│         SUPER ADMIN                 │
│  ├─ All GROUP.* permissions         │
│  └─ All SYSTEM.LOGS.VIEW            │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│            ADMIN                    │
│  ├─ USER.VIEW, USER.MODIFY          │
│  ├─ SUBSCRIPTION.VIEW               │
│  ├─ PLAN.VIEW, PLAN.MODIFY          │
│  ├─ PROMO.VIEW, PROMO.MODIFY        │
│  ├─ SYSTEM.LOGS.VIEW                │
│  └─ NOTIF.SEND                      │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│       AUTHENTICATED USER            │
│  └─ CV.CREATE                       │
└─────────────────────────────────────┘
```

## Best Practices

1. **Least Privilege**: Assign the minimum permissions required for a user's role
2. **Separation of Duties**: Use `VIEW` vs `MODIFY` to separate read and write access
3. **Audit Critical Actions**: `SYSTEM.LOGS.VIEW` should be restricted to trusted administrators
4. **Group Management**: `GROUP.MODIFY` should only be granted to Super Admins

## Future Enhancements

1. **Split CV.CREATE permission** (High Priority):
   - `CV.UPLOAD` - Upload and parse CV files
   - `CV.TAILOR` - Tailor CVs to job descriptions
   - `CV.EXPORT` - Export CVs in various formats (PDF, DOCX)
2. Add `PLAN.DELETE` as separate permission from `PLAN.MODIFY`
3. Introduce `USER.DELETE` for user account management
