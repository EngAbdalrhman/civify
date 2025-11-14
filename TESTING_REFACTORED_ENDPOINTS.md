# Testing Guide: Refactored User Filter Endpoints

## Prerequisites

1. **Compile the project:**
   ```bash
   mvn clean compile
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Get authentication token:**
   - Login as admin user
   - Copy the JWT token from the response

---

## Endpoint 1: Filter Users (Advanced)

### Request
```http
POST http://localhost:8077/api/v1/admin/users/filter
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "filters": [
    {"fieldName": "email", "operation": "MATCH", "value": "example.com"}
  ],
  "filterGroups": [],
  "page": 0,
  "size": 10
}
```

### Expected Response
```json
{
  "status": "success",
  "message": "Users filtered successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "username": "john_doe",
        "email": "john@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "role": "USER",
        "emailVerified": true,
        "createdAt": "2025-11-14T10:00:00",
        "updatedAt": "2025-11-14T10:00:00"
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
  },
  "timestamp": "2025-11-14T15:56:08.377",
  "requestId": "c9b1dfd4-59cf-4f43-b9fb-d79ffbd0acfe"
}
```

### More Filter Examples

**Filter by role:**
```json
{
  "filters": [
    {"fieldName": "role", "operation": "EQUAL", "value": "ADMIN"}
  ],
  "page": 0,
  "size": 10
}
```

**Filter by email verified:**
```json
{
  "filters": [
    {"fieldName": "emailVerified", "operation": "EQUAL", "value": "true"}
  ],
  "page": 0,
  "size": 10
}
```

**Multiple filters (AND logic):**
```json
{
  "filters": [
    {"fieldName": "role", "operation": "EQUAL", "value": "USER"},
    {"fieldName": "emailVerified", "operation": "EQUAL", "value": "true"}
  ],
  "page": 0,
  "size": 10
}
```

**Complex filter with groups (OR logic):**
```json
{
  "filters": [],
  "filterGroups": [
    {
      "filters": [
        {"fieldName": "email", "operation": "MATCH", "value": "gmail.com"}
      ],
      "logicalOperation": "OR"
    },
    {
      "filters": [
        {"fieldName": "email", "operation": "MATCH", "value": "yahoo.com"}
      ],
      "logicalOperation": "OR"
    }
  ],
  "page": 0,
  "size": 10
}
```

---

## Endpoint 2: Get All Users (Simple Pagination)

### Request
```http
GET http://localhost:8077/api/v1/admin/users?page=0&size=10
Authorization: Bearer YOUR_JWT_TOKEN
```

### Expected Response
```json
{
  "status": "success",
  "message": "Users retrieved successfully",
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 50,
    "totalPages": 5,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2025-11-14T15:56:08.377",
  "requestId": "..."
}
```

### Pagination Examples

**First page (default):**
```http
GET http://localhost:8077/api/v1/admin/users
```

**Second page:**
```http
GET http://localhost:8077/api/v1/admin/users?page=1&size=10
```

**Larger page size:**
```http
GET http://localhost:8077/api/v1/admin/users?page=0&size=50
```

---

## Endpoint 3: Get User by ID

### Request (Without Statistics)
```http
GET http://localhost:8077/api/v1/admin/users/1
Authorization: Bearer YOUR_JWT_TOKEN
```

### Expected Response
```json
{
  "status": "success",
  "message": "User retrieved successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER",
    "emailVerified": true,
    "createdAt": "2025-11-14T10:00:00",
    "updatedAt": "2025-11-14T10:00:00"
  },
  "timestamp": "2025-11-14T15:56:08.377",
  "requestId": "..."
}
```

### Request (With Statistics)
```http
GET http://localhost:8077/api/v1/admin/users/1?includeStats=true
Authorization: Bearer YOUR_JWT_TOKEN
```

### Expected Response
```json
{
  "status": "success",
  "message": "User retrieved successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER",
    "emailVerified": true,
    "activeSubscriptionCount": 1,
    "totalSubscriptionCount": 3,
    "createdAt": "2025-11-14T10:00:00",
    "updatedAt": "2025-11-14T10:00:00"
  },
  "timestamp": "2025-11-14T15:56:08.377",
  "requestId": "..."
}
```

---

## Error Responses

### User Not Found (404)
```json
{
  "status": "error",
  "message": "User not found with ID: 999",
  "data": null,
  "timestamp": "2025-11-14T15:56:08.377",
  "requestId": "..."
}
```

### Invalid Filter Criteria (400)
```json
{
  "status": "error",
  "message": "Invalid filter field: invalidField",
  "data": null,
  "timestamp": "2025-11-14T15:56:08.377",
  "requestId": "..."
}
```

### Unauthorized (401)
```json
{
  "status": "error",
  "message": "Unauthorized",
  "data": null,
  "timestamp": "2025-11-14T15:56:08.377",
  "requestId": "..."
}
```

### Forbidden (403)
```json
{
  "status": "error",
  "message": "Access denied. Admin role required.",
  "data": null,
  "timestamp": "2025-11-14T15:56:08.377",
  "requestId": "..."
}
```

---

## Available Filter Operations

- `EQUAL` - Exact match
- `NOT_EQUAL` - Not equal
- `MATCH` - Contains (case-insensitive)
- `GREATER_THAN` - Greater than
- `LESS_THAN` - Less than
- `GREATER_THAN_OR_EQUAL` - Greater than or equal
- `LESS_THAN_OR_EQUAL` - Less than or equal
- `IN` - In list
- `NOT_IN` - Not in list
- `IS_NULL` - Is null
- `IS_NOT_NULL` - Is not null
- `BETWEEN` - Between two values
- `STARTS_WITH` - Starts with
- `ENDS_WITH` - Ends with

---

## Notes

1. **Authentication Required:** All endpoints require a valid JWT token with ADMIN role
2. **Pagination:** Page numbers are 0-based (first page = 0)
3. **Default Values:** If not specified, `page=0` and `size=10`
4. **Sorting:** The `getAllUsers` endpoint sorts by `createdAt` DESC by default
5. **Statistics:** Use `includeStats=true` to get subscription counts in user details

---

## Troubleshooting

### Issue: "Cannot find symbol: log"
**Solution:** This is a Lombok IDE error. Compile with Maven - it will work fine.

### Issue: "Unauthorized" response
**Solution:** Make sure you're sending the JWT token in the Authorization header.

### Issue: "Access denied"
**Solution:** Make sure your user has the ADMIN role.

### Issue: Empty results
**Solution:** Check if you have users in the database that match your filter criteria.

