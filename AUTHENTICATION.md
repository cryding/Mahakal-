# MAHAKAL Authentication System Architecture (Phase 2)

## 1. Overview & Core Tenet
MAHAKAL enforces a strict closed-loop hierarchical security model:
```
ADMIN
  ↓
AGENT
  ↓
USER
```
There is **NO public registration**, **NO self-sign-up**, **NO OTP registration**, **NO phone verification**, and **NO email registration**.
All accounts are provisioned exclusively by their designated parent authority:
- **Master Admin**: Bootstrapped securely via environment secrets.
- **Agents**: Provisioned exclusively by Admin.
- **Users**: Provisioned exclusively by their supervising Agent.

The ONLY authentication method across all roles is:
**ID / Username + Password**

---

## 2. Cryptographic Password Security
- **Algorithm**: PBKDF2 with HMAC-SHA256 (`PBKDF2WithHmacSHA256`)
- **Key Derivation Iterations**: 100,000 iterations
- **Key Length**: 256 bits
- **Salt Generation**: 16-byte cryptographically secure pseudorandom salt (`SecureRandom`), generated uniquely per account and stored as Hex.
- **Verification Strategy**: Constant-time byte array comparison using `MessageDigest.isEqual` to eliminate timing side-channel attacks.
- **In-Memory Hygiene**: Password char arrays are cleared immediately following cryptographic hashing.
- **Zero Plaintext**: Passwords are never stored in plaintext, never logged to console/logcat, never included in crash dumps, and never written to unsecured preferences.

---

## 3. Account Model & Status Enforcement
Each account entity maintains:
- `id`: Internal immutable UUID (Primary Key)
- `loginId`: Alphanumeric unique identifier (Unique Index)
- `passwordHash`: Salted PBKDF2 hash
- `salt`: Hex-encoded 16-byte salt
- `role`: Strictly `ADMIN`, `AGENT`, or `USER`
- `status`: Strictly `ACTIVE`, `SUSPENDED`, or `DISABLED`
- `parentId`: UUID of managing parent (Agent's adminId, User's agentId)
- `fullName`: Display name
- `createdAt`: UTC Unix epoch timestamp
- `updatedAt`: UTC Unix epoch timestamp
- `lastLoginAt`: Timestamp of latest successful authentication
- `passwordChangedAt`: Timestamp of last password update
- `mustChangePassword`: Boolean flag requiring mandatory change upon initial login
- `failedLoginAttempts`: Counter for failed attempts
- `lockedUntil`: Timestamp until which login attempts are barred

### Status Enforcement Rules
- **ACTIVE**: Authentication permitted; active sessions valid.
- **SUSPENDED**: Authentication denied immediately with HTTP `403 Forbidden` (`ACCOUNT_SUSPENDED`). Any existing sessions are immediately revoked server-side.
- **DISABLED**: Permanent account deactivation. Access rejected with HTTP `403 Forbidden` (`ACCOUNT_DISABLED`).

---

## 4. Session & Token System
- **Access Tokens**: High-entropy cryptographically generated tokens (`mhk_acc_<uuid><entropy>`), valid for 1 hour.
- **Refresh Tokens**: Cryptographically generated refresh tokens (`mhk_ref_<uuid><entropy>`), valid for 7 days.
- **Server-Side Session Store**: Active sessions are persisted in the `sessions` table (`MahakalServerDatabase`).
- **Token Revocation**:
  - Calling `/auth/logout` sets `is_revoked = 1` immediately.
  - Changing account status to `SUSPENDED` or `DISABLED` revokes all active sessions for that account.
- **Android Client Storage**: Stored securely using `SecureTokenStorage` backed by Android KeyStore with AES-256 GCM encryption.
