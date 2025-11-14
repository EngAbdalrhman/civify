# Testing Guide for Filter Endpoints

## Issue Fixed

The compilation error was due to using wrong method names:
- ❌ `setCurrentPage()` and `setPageSize()` (don't exist)
- ✅ `setPage()` and `setSize()` (correct)

The controller has been updated to use the correct methods.

---

## Testing the Filter Endpoint

### Step 1: Compile the Application

```bash
mvn clean compile
```

This will resolve the Lombok IDE errors (they are false positives).

### Step 2: Run the Application

```bash
mvn spring-boot:run
```

Or run from your IDE.

### Step 3: Authenticate (Get JWT Token)

**Endpoint:** `POST http://localhost:8077/api/v1/auth/login`

**Request Body:**
```json
{
  "username": "admin",
  "password": "your_admin_password"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "...",
  "requires2FA": false,
  "message": "Login successful"
}
```

Copy the `accessToken` value.

### Step 4: Test the Filter Endpoint

**Endpoint:** `POST http://localhost:8077/api/v1/admin/users/filter`

**Headers:**
```
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE
Content-Type: application/json
```

**Request Body (Simple):**
```json
{
  "filters": [
    {"fieldName": "email", "operation": "MATCH", "value": "example.com"}
  ],
  "page": 0,
  "size": 10
}
```

**Expected Response:**
```json
{
  "content": [
    {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "role": "ROLE_USER",
      "isEmailVerified": true,
      ...
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

---

## Common Test Cases

### 1. Filter by Email Domain

```json
{
  "filters": [
    {"fieldName": "email", "operation": "MATCH", "value": "gmail.com"}
  ],
  "page": 0,
  "size": 10
}
```

### 2. Filter by Role

```json
{
  "filters": [
    {"fieldName": "role", "operation": "EQUAL", "value": "ROLE_USER"}
  ],
  "page": 0,
  "size": 10
}
```

### 3. Filter Verified Users

```json
{
  "filters": [
    {"fieldName": "isEmailVerified", "operation": "EQUAL", "value": true}
  ],
  "page": 0,
  "size": 10
}
```

### 4. Multiple Filters (AND)

```json
{
  "filters": [
    {"fieldName": "email", "operation": "MATCH", "value": "gmail"},
    {"fieldName": "isEmailVerified", "operation": "EQUAL", "value": true},
    {"fieldName": "role", "operation": "EQUAL", "value": "ROLE_USER"}
  ],
  "page": 0,
  "size": 10
}
```

### 5. With Sorting

```json
{
  "filters": [
    {"fieldName": "isEmailVerified", "operation": "EQUAL", "value": true}
  ],
  "sorts": [
    {"fieldName": "createdAt", "direction": "DESC"},
    {"fieldName": "email", "direction": "ASC"}
  ],
  "page": 0,
  "size": 10
}
```

### 6. Pagination (Second Page)

```json
{
  "filters": [],
  "page": 1,
  "size": 10
}
```

### 7. Complex Query with Filter Groups

Find users where (email contains "gmail" OR "yahoo") AND role is USER:

```json
{
  "filterGroups": [
    {
      "operation": "OR",
      "filters": [
        {"fieldName": "email", "operation": "MATCH", "value": "gmail"},
        {"fieldName": "email", "operation": "MATCH", "value": "yahoo"}
      ]
    }
  ],
  "filters": [
    {"fieldName": "role", "operation": "EQUAL", "value": "ROLE_USER"}
  ],
  "operation": "AND",
  "page": 0,
  "size": 10
}
```

---

## Understanding Your Current Request

Your request:
```json
{
  "filters": [
    {"fieldName": "email", "operation": "MATCH", "value": "example.com"}
  ],
  "filterGroups": [],
  "page": 1,
  "size": 10
}
```

**What it does:**
- Finds users where email contains "example.com" (case-insensitive)
- Returns page 1 (items 11-20)
- 10 items per page

**Note:** `page: 1` means the SECOND page (0-based indexing)
- `page: 0` = First 10 items (1-10)
- `page: 1` = Second 10 items (11-20)
- `page: 2` = Third 10 items (21-30)

If you want the first page, use `page: 0`.

---

## Troubleshooting

### Error: "ANONYMOUS_REQUEST | no authentication"

**Cause:** You're not authenticated.

**Solution:** 
1. Login to get JWT token
2. Add `Authorization: Bearer YOUR_TOKEN` header to your request

### Error: "Access Denied" or "403 Forbidden"

**Cause:** Your user doesn't have ROLE_ADMIN.

**Solution:** 
1. Make sure you're logged in as an admin user
2. Or temporarily comment out `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` for testing

### Error: "Unresolved compilation problems"

**Cause:** IDE Lombok errors (false positives).

**Solution:** 
1. Run `mvn clean compile` - it will compile successfully
2. Ignore IDE errors - they're Lombok-related false positives

### No Results Returned

**Cause:** No users match your filter criteria.

**Solution:**
1. Try a broader filter (e.g., remove some filters)
2. Check if you have any users in the database
3. Try `GET /api/v1/admin/users` to see all users first

---

## Postman Collection

Here's a Postman collection you can import:

```json
{
  "info": {
    "name": "User Filter API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Login",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"username\": \"admin\",\n  \"password\": \"admin123\"\n}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "http://localhost:8077/api/v1/auth/login",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8077",
          "path": ["api", "v1", "auth", "login"]
        }
      }
    },
    {
      "name": "Filter Users",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{accessToken}}",
            "type": "text"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"filters\": [\n    {\"fieldName\": \"email\", \"operation\": \"MATCH\", \"value\": \"example.com\"}\n  ],\n  \"page\": 0,\n  \"size\": 10\n}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "http://localhost:8077/api/v1/admin/users/filter",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8077",
          "path": ["api", "v1", "admin", "users", "filter"]
        }
      }
    }
  ]
}
```

Save the access token from the login response and use it in the filter request!

