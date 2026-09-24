# MAHAKAL System Architecture & Security Manual

**Application**: MAHAKAL Enterprise Virtual Coin Management & Prediction Platform  
**Target Environment**: Production  
**Document Classification**: Enterprise Architecture & Security Standard  

---

## 1. High-Level Architectural Overview

MAHAKAL is structured around a Clean Architecture / MVVM pattern on the Android front-end, coupled to a hardened, authoritative, microservices-style server engine backed by Room/SQLite and relational databases.

```
+-------------------------------------------------------------------------+
|                         MAHAKAL Android Client                          |
|  (Jetpack Compose UI, Material 3, ViewModel, Repositories, DataSources) |
+------------------------------------+------------------------------------+
                                     |
                                HTTPS / TLS 1.3
                          Encrypted Bearer Tokens
                                     |
+------------------------------------v------------------------------------+
|                         Server API Router                               |
|        (Request Validation, Rate Limiter, Security Filter Pipeline)      |
+---------+-------------------+-------------------+-------------------+---+
          |                   |                   |                   |
+---------v---------+ +-------v---------+ +-------v---------+ +-------v---+
| ServerAuthService | | AdminAgentSvc   | | WalletLedgerSvc | | GameEngine|
|  - Argon2id Hash  | | AgentUserSvc    | | - Double-Entry  | | - State   |
|  - Token Rotation | | - Tenant Guard  | | - Idempotency   | |   Machine |
|  - Reuse Alert    | | - Scope Checks  | | - Checkpoints   | | - Settle  |
+---------+---------+ +-------+---------+ +-------+---------+ +-------+---+
          |                   |                   |                   |
          +-------------------+---------+---------+-------------------+
                                        |
                 +----------------------v---------------------+
                 |       Transactional Outbox Worker          |
                 |  - Reliable event delivery & notification  |
                 +----------------------+---------------------+
                                        |
                 +----------------------v---------------------+
                 |        MahakalServerDatabase (Room v8)     |
                 |  (Accounts, Wallets, Ledger, Games, Logs)  |
                 +--------------------------------------------+
```

---

## 2. Core Subsystems

### 2.1 Identity, Authentication & Sessions
- **Password Protection**: Uses Argon2id key derivation with cryptographic salt generation (`PasswordHasher.kt`). Plaintext passwords never touch logs or persistent records.
- **Session Tokens**: 
  - Ephemeral Access Tokens with 1-hour expiration.
  - Refresh Tokens featuring automatic token rotation on every exchange.
  - **Token Family Reuse Detection**: If an old or invalidated refresh token is re-submitted, the entire session family for that account is revoked immediately and flagged in the security audit.
- **Brute Force Protection**: In-memory and persisted rate limiters track failed attempts per login ID and IP address, triggering exponential lockout thresholds.

### 2.2 Role-Based Access Control (RBAC) & Tenant Isolation
The platform implements three distinct roles with strict vertical and horizontal isolation:
1. **ADMIN**: Master supervisory authority over platform configurations, system games, all agents, and financial ledgers.
2. **AGENT**: Sub-supervisory authority strictly constrained to accounts created under their own hierarchical agent ID (`parentId = agentId`). Agents cannot query or transact with users under other agents.
3. **USER**: End-player access strictly constrained to personal wallet, active games, own submitted predictions, and received notifications.

### 2.3 Authoritative Wallet & Double-Entry Ledger
- **Authoritative Balance Invariant**: The client never calculates or determines balances. Balance values are strictly derived from server-verified ledger states.
- **Double-Entry Operations**: Every coin movement requires an explicit source, destination, transaction type, reason, balance-before, and balance-after checkpoint.
- **Idempotency Guarantees**: Mutations require a client-generated or server-scoped `idempotencyKey` preventing duplicate balance deductions or transfers under network re-transmissions.

### 2.4 Prediction Game Lifecycle Engine
- **State Machine**:
  - `DRAFT` / `SCHEDULED`: Configured by Admin; entries not yet accepted.
  - `OPEN`: Actively accepting prediction entries until `entryDeadline`.
  - `CLOSED`: Deadline elapsed; entries strictly rejected.
  - `RESULT_PENDING`: Awaiting authoritative outcome submission.
  - `RESULT_FINALIZED`: Outcome declared, rewards idempotently disbursed.
  - `CANCELLED`: Entries automatically refunded to participant wallets.

### 2.5 Outbox Worker & Notifications
- Events are saved transactionally in the `outbox_events` table within the same database transaction as the business operation.
- The `OutboxWorker` executes asynchronously, reading pending events, delivering in-app alerts, updating read states, and managing retries.

---

## 3. Incident Response & Disaster Recovery
- **Security Incidents**: Detected security events (unauthorized role access, token reuse, rate limit breaches) trigger immediate entries in `security_events` and `audit_logs` with correlation IDs.
- **Database Backup & Recovery**:
  - Database migrations (v1 through v8) maintain zero-loss forward schema upgrades with explicit indices.
  - *Operational DR Note*: While the automated backup procedure is documented in `DEPLOYMENT.md`, formal end-to-end restore verification in a production cluster environment has not yet been executed (**RESTORE TEST NOT VERIFIED** in live cloud cluster).
