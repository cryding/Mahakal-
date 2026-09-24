# MAHAKAL Server Authentication & RBAC API Specification (Phase 2)

## 1. Standard Request & Response Envelopes

### Standard Error Response Envelope
```json
{
  "success": false,
  "statusCode": 401,
  "errorCode": "INVALID_CREDENTIALS",
  "message": "Invalid ID or password.",
  "requestId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

### Standard Status Codes
- `200 OK`: Request succeeded.
- `201 Created`: Resource successfully provisioned.
- `401 Unauthorized`: Authentication missing, expired, or invalid credentials.
- `403 Forbidden`: Account suspended, disabled, or lacking required role/permission.
- `409 Conflict`: Unique constraint violation (e.g. login ID collision).
- `422 Unprocessable Entity`: Input validation failure.
- `429 Too Many Requests`: Rate limit exceeded (lockout active).
- `500 Internal Server Error`: Internal system error.

---

## 2. Authentication Endpoints

### 2.1 Login
- **Endpoint**: `POST /auth/login`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "loginId": "admin",
    "password": "AdminPassword@123"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "statusCode": 200,
    "data": {
      "accessToken": "mhk_acc_39af8b2...",
      "refreshToken": "mhk_ref_17c09d...",
      "expiresIn": 3600,
      "user": {
        "id": "c7a86f91-5a81-4b13-a419-756ef26e0811",
        "loginId": "admin",
        "role": "ADMIN",
        "status": "ACTIVE",
        "fullName": "Mahakal Chief Administrator",
        "mustChangePassword": false
      }
    },
    "requestId": "8f89e4c1-92b0-4660-84cf-2d6db9fe09bc"
  }
  ```
- **Error Responses**:
  - `401 Unauthorized`: `INVALID_CREDENTIALS` ("Invalid ID or password.")
  - `403 Forbidden`: `ACCOUNT_SUSPENDED` ("Account has been suspended.")
  - `422 Unprocessable Entity`: `VALIDATION_ERROR` ("Login ID and Password are required.")
  - `429 Too Many Requests`: `RATE_LIMIT_EXCEEDED` ("Too many failed attempts. Temporary lockout in effect.")

### 2.2 Get Authenticated Profile
- **Endpoint**: `GET /auth/me`
- **Headers**:
  - `Authorization: Bearer <accessToken>`
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "statusCode": 200,
    "data": {
      "id": "c7a86f91-5a81-4b13-a419-756ef26e0811",
      "loginId": "admin",
      "role": "ADMIN",
      "status": "ACTIVE",
      "fullName": "Mahakal Chief Administrator",
      "mustChangePassword": false
    },
    "requestId": "c4d380e2-d4b9-4a0b-9ef1-18e388d05e21"
  }
  ```

### 2.3 Logout
- **Endpoint**: `POST /auth/logout`
- **Headers**:
  - `Authorization: Bearer <accessToken>`
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "statusCode": 200,
    "data": null,
    "message": "Session successfully invalidated.",
    "requestId": "2f6c91d8-04e2-4521-a53b-e1c50058b8f3"
  }
  ```

### 2.4 Change Password
- **Endpoint**: `POST /auth/change-password`
- **Headers**:
  - `Authorization: Bearer <accessToken>`
  - `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "currentPassword": "OldPassword@123",
    "newPassword": "NewStrongPassword@2026"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "statusCode": 200,
    "data": null,
    "message": "Password successfully updated.",
    "requestId": "5e1c5005-8b8f-4521-a53b-04e22f6c91d8"
  }
  ```
- **Error Responses**:
  - `401 Unauthorized`: `INVALID_CREDENTIALS` ("Current password verification failed.")
  - `422 Unprocessable Entity`: `WEAK_PASSWORD` ("Password must be at least 8 characters long.")
