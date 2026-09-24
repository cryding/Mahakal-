# MAHAKAL Security & Threat Mitigation Specification (Phase 2)

## 1. Threat Mitigation Matrix

| Threat Vector | Mitigation Strategy | Implementation Location |
| :--- | :--- | :--- |
| **Credential Stuffing & Brute Force** | Rate limiting tracking failed attempts per login ID and IP address (5 failed attempts within 5 minutes results in a 15-minute temporary lockout). | `RateLimiter.kt`, `ServerAuthService.kt` |
| **Account Enumeration** | Constant generic error responses: `"Invalid ID or password."` returned identically for both non-existent users and incorrect passwords with uniform status `401`. | `ServerAuthService.kt` |
| **Timing Attacks** | Constant-time hash verification via `MessageDigest.isEqual()`. | `PasswordHasher.kt` |
| **Client Role Spoofing** | The server derives identity and role strictly from the verified session token in the `Authorization: Bearer <token>` header. Any client-sent role, user ID, or hierarchy param is ignored. | `ServerApiRouter.kt`, `MahakalRbac.kt` |
| **Lateral Privilege Escalation** | Agent resource endpoints strictly enforce `validateResourceOwnership(context, targetOwnerId, targetAgentId)`. Agent 1 cannot query, update, or view Agent 2's subordinated users. | `MahakalRbac.kt`, `ServerApiRouter.kt` |
| **Vertical Privilege Escalation** | `requireRole(context, AccountRole.ADMIN)` and `requirePermission(context, permission)` reject unauthorized roles with HTTP `403 Forbidden`. | `MahakalRbac.kt` |
| **Session Hijacking / Stale Sessions** | Short-lived access tokens (1 hour), immediate server-side revocation on logout, automatic revocation upon account suspension. | `SessionDao.kt`, `ServerAuthService.kt` |
| **Sensitive Data Exposure** | Plaintext passwords, bearer tokens, and secrets are strictly redacted before writing to audit logs or error logs. | `AuditService.kt` |

---

## 2. Cryptographic Configuration
- **Hashing**: PBKDF2WithHmacSHA256
- **Work Factor**: 100,000 iterations
- **Key Length**: 256 bits
- **Salt**: 128-bit (16-byte) cryptographically secure pseudorandom number
- **Token Entropy**: 128-bit UUID + 96-bit random hex entropy

---

## 3. Account Status Middleware Guard
Before any protected endpoint executes business logic:
1. `validateSession(token)` checks if the session is revoked or expired.
2. The account's authoritative current status is fetched from the persistent `accounts` table.
3. If `status != ACTIVE`, all sessions for the account are revoked, and access is denied with `403 Forbidden`.
