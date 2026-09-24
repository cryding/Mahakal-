# MAHAKAL ARCHITECTURE SPECIFICATION — PHASE 1
**System Classification**: Enterprise Hierarchical Accounting & Gameplay Platform (Non-Monetary Virtual Coin Ledger)  
**Strict Domain Constraint**: Internal gameplay/accounting units only. Absolute prohibition against fiat/INR conversion, payment gateways, banking/UPI/crypto withdrawals, or cash redemption.

---

## A. COMPLETE ARCHITECTURE DIAGRAM

```
+========================================================================================+
|                                    MAHAKAL SYSTEM                                      |
+========================================================================================+

                               +----------------------------------+
                               |     Android Client (MAHAKAL)     |
                               |    Kotlin + Jetpack Compose M3   |
                               +-----------------+----------------+
                                                 |
                                                 | HTTPS / TLS 1.3 (Certificate Pinned)
                                                 | Header: Authorization Bearer <JWT>
                                                 | Header: X-Idempotency-Key <UUID>
                                                 | Header: X-Request-ID <UUID>
                                                 v
+----------------------------------------------------------------------------------------+
|                             API GATEWAY / EDGE PROXY (WAF)                            |
|  - Rate Limiting (Token Bucket per IP / Actor)   - DDoS Mitigation                     |
|  - TLS Termination & Strict Cipher Suite         - Request Correlation ID Injection    |
+------------------------------------------------+---------------------------------------+
                                                 |
                                                 v
+----------------------------------------------------------------------------------------+
|                          MAHAKAL CORE APPLICATION SERVICES                             |
|                                                                                        |
|  +---------------------+  +----------------------+  +-------------------------------+  |
|  | Authentication &    |  | RBAC & Hierarchical  |  | Virtual Coin Ledger Engine    |  |
|  | Session Service     |  | Enforcement Policy   |  | (ACID Atomic Double-Entry)    |  |
|  | - Argon2id Hasher   |  | - Admin Scope        |  | - Optimistic Concurrency      |  |
|  | - JWT Mint/Revoke   |  | - Agent Ownership    |  | - Idempotency Lock Manager    |  |
|  | - Session Revocation|  | - User Boundary      |  | - Invariant Validator (>= 0)  |  |
|  +---------------------+  +----------------------+  +-------------------------------+  |
|                                                                                        |
|  +---------------------+  +----------------------+  +-------------------------------+  |
|  | Account Lifecycle   |  | Prediction / Match   |  | Audit & Compliance Engine     |  |
|  | Service             |  | Gameplay Engine      |  | - Append-only tamper-evident  |  |
|  | - Admin -> Agent    |  | - Match Scheduler    |  |   hash-chained audit logs     |  |
|  | - Agent -> User     |  | - Non-monetary wager |  | - Structured JSON telemetry   |  |
|  | - Status Lifecycle  |  | - Settlement Lock    |  | - Security Incident Alerter   |  |
|  +---------------------+  +----------------------+  +-------------------------------+  |
+----------------------+--------------------+---------------------+----------------------+
                       |                    |                     |
                       v                    v                     v
         +------------------------+  +----------------+  +--------------------+
         | Cloud SQL (PostgreSQL) |  | Redis Cluster  |  | Cloud KMS / Secret |
         | Primary (HA) + Replica |  | - Idempotency  |  | Manager            |
         | Serializable Isolation |  |   Locks (TTL)  |  | - JWT Signing Keys |
         | Foreign Keys & Indexes |  | - Revocation   |  | - DB Credentials   |
         | Immutable Ledger Row   |  |   Blocklist    |  | - Salt Pepper Keys |
         +------------------------+  +----------------+  +--------------------+
```

---

## B. DATABASE ER MODEL

```
   +-------------------+              +-------------------+              +-------------------+
   |      admins       | 1          * |      agents       | 1          * |       users       |
   +-------------------+--------------+-------------------+--------------+-------------------+
   | PK id (UUID)      |              | PK id (UUID)      |              | PK id (UUID)      |
   | UK username (V255)|              | UK agent_id (V64) |              | UK user_id (V64)  |
   |    password_hash  |              | FK admin_id (UUID)|              | FK agent_id (UUID)|
   |    full_name      |              |    agent_name     |              |    display_name   |
   |    status (ENUM)  |              |    password_hash  |              |    password_hash  |
   |    must_change_pwd|              |    status (ENUM)  |              |    status (ENUM)  |
   |    created_at     |              |    must_change_pwd|              |    must_change_pwd|
   |    updated_at     |              |    notes          |              |    notes          |
   +---------+---------+              |    created_at     |              |    created_at     |
             |                        |    updated_at     |              |    updated_at     |
             |                        +---------+---------+              +---------+---------+
             | 1:1                              | 1:1                              | 1:1
             +--------------------+             +------------------+               |
                                  |                                |               |
                                  v                                v               v
                       +---------------------------------------------------------------+
                       |                            wallets                            |
                       +---------------------------------------------------------------+
                       | PK id (UUID)                                                  |
                       | UK owner_id (UUID)                                            |
                       |    owner_role (ENUM: ADMIN, AGENT, USER)                      |
                       |    balance (BIGINT NOT NULL CHECK (balance >= 0))             |
                       |    currency_type (VARCHAR = 'VIRTUAL_COIN')                   |
                       |    version (BIGINT NOT NULL DEFAULT 0)                        |
                       |    created_at (TIMESTAMPTZ)                                   |
                       |    updated_at (TIMESTAMPTZ)                                   |
                       +-------------------------------+-------------------------------+
                                                       |
                             +-------------------------+-------------------------+
                             | 1 (Source)                                        | 1 (Destination)
                             v                                                   v
   +-----------------------------------------------------------------------------------+
   |                                wallet_transactions                                |
   +-----------------------------------------------------------------------------------+
   | PK id (UUID)                                                                      |
   | UK idempotency_key (VARCHAR(128))                                                 |
   |    actor_id (UUID NOT NULL)                                                       |
   |    actor_role (ENUM: ADMIN, AGENT, USER, SYSTEM)                                  |
   | FK source_wallet_id (UUID REFERENCES wallets(id))                                 |
   | FK destination_wallet_id (UUID REFERENCES wallets(id))                            |
   |    amount (BIGINT NOT NULL CHECK (amount > 0))                                    |
   |    balance_before_source (BIGINT)                                                 |
   |    balance_after_source (BIGINT)                                                  |
   |    balance_before_dest (BIGINT)                                                   |
   |    balance_after_dest (BIGINT)                                                    |
   |    tx_type (ENUM: ADMIN_TO_AGENT, AGENT_TO_USER, ADMIN_DEDUCT, AGENT_DEDUCT, etc) |
   |    reason (TEXT)                                                                  |
   |    reference_id (VARCHAR(128))                                                    |
   |    metadata (JSONB)                                                               |
   |    created_at (TIMESTAMPTZ NOT NULL DEFAULT NOW())                                |
   +-----------------------------------------------------------------------------------+

   +---------------------------------------+      +------------------------------------+
   |               sessions                |      |             audit_logs             |
   +---------------------------------------+      +------------------------------------+
   | PK id (UUID)                          |      | PK id (UUID)                       |
   |    account_id (UUID NOT NULL)         |      |    actor_id (UUID NOT NULL)        |
   |    role (ENUM: ADMIN, AGENT, USER)    |      |    actor_role (ENUM)               |
   |    refresh_token_hash (VARCHAR(255))  |      |    action (VARCHAR(64) NOT NULL)   |
   |    user_agent (TEXT)                  |      |    target_id (UUID)                |
   |    ip_address (INET)                  |      |    target_type (VARCHAR(32))       |
   |    is_revoked (BOOLEAN DEFAULT FALSE) |      |    request_id (VARCHAR(64))        |
   |    expires_at (TIMESTAMPTZ NOT NULL)  |      |    before_state (JSONB)            |
   |    created_at (TIMESTAMPTZ)           |      |    after_state (JSONB)             |
   +---------------------------------------+      |    metadata (JSONB)                |
                                                  |    created_at (TIMESTAMPTZ)        |
                                                  +------------------------------------+

   +---------------------------------------+      +------------------------------------+
   |                matches                | 1  * |            predictions             |
   +---------------------------------------+      +------------------------------------+
   | PK id (UUID)                          |      | PK id (UUID)                       |
   |    title (VARCHAR(255))               |      | FK match_id (UUID)                 |
   |    category (VARCHAR(64))             |      | FK user_id (UUID)                  |
   |    scheduled_start (TIMESTAMPTZ)      |      | FK wallet_id (UUID)                |
   |    lock_time (TIMESTAMPTZ)            |      |    option_selected (VARCHAR(64))   |
   |    status (SCHEDULED, LOCKED, SETTLED)|      |    coins_staked (BIGINT > 0)       |
   |    result_summary (TEXT)              |      |    status (PENDING, WON, LOST)     |
   |    created_at (TIMESTAMPTZ)           |      |    coins_awarded (BIGINT DEFAULT 0)|
   +---------------------------------------+      |    created_at (TIMESTAMPTZ)        |
                                                  +------------------------------------+
```

---

## C. COMPLETE RBAC MATRIX

| Resource & Operation | Required Permission | ADMIN | AGENT | USER | Scope & Hierarchy Validation Rule |
|---|---|:---:|:---:|:---:|---|
| **Admin Operations** | | | | | |
| Create Agent | `ADMIN_MANAGE_AGENTS` | ✅ | ❌ | ❌ | Admin only; allocates agent under authenticated admin |
| View / Search All Agents | `ADMIN_MANAGE_AGENTS` | ✅ | ❌ | ❌ | Global access to all agents |
| Edit Agent Profile | `ADMIN_MANAGE_AGENTS` | ✅ | ❌ | ❌ | Admin only |
| Suspend / Activate Agent | `ADMIN_MANAGE_AGENTS` | ✅ | ❌ | ❌ | Cascades session invalidation to target agent |
| Reset Agent Password | `ADMIN_MANAGE_AGENTS` | ✅ | ❌ | ❌ | Generates temporary password; sets must_change_pwd |
| Allocate Coins to Agent | `ADMIN_MANAGE_AGENT_COINS`| ✅ | ❌ | ❌ | Deducts Admin wallet, credits Agent wallet atomically |
| Deduct Coins from Agent | `ADMIN_MANAGE_AGENT_COINS`| ✅ | ❌ | ❌ | Deducts Agent wallet, refunds Admin wallet atomically |
| View Global Reports | `ADMIN_VIEW_REPORTS` | ✅ | ❌ | ❌ | System-wide statistics and coin circulation metrics |
| View Full Audit Logs | `ADMIN_VIEW_AUDIT` | ✅ | ❌ | ❌ | Immutable append-only audit stream across all actors |
| **Agent Operations** | | | | | |
| Create User | `AGENT_CREATE_USER` | ❌ | ✅ | ❌ | Target user assigned to authenticated agent ONLY |
| View Own Users | `AGENT_VIEW_OWN_USERS` | ❌ | ✅ | ❌ | Strictly filtered by `user.agent_id == agent.id` |
| Edit Own User | `AGENT_MANAGE_OWN_USERS` | ❌ | ✅ | ❌ | Ownership check enforced; Agent cannot edit others' users |
| Suspend / Activate Own User| `AGENT_MANAGE_OWN_USERS`| ❌ | ✅ | ❌ | Must belong to agent; invalidates user session |
| Reset Own User Password | `AGENT_MANAGE_OWN_USERS` | ❌ | ✅ | ❌ | Must belong to agent; temporary password issued |
| Allocate Coins to Own User | `AGENT_MANAGE_OWN_USER_COINS`| ❌ | ✅ | ❌ | Deducts Agent wallet, credits User wallet atomically |
| Deduct Coins from Own User | `AGENT_MANAGE_OWN_USER_COINS`| ❌ | ✅ | ❌ | Deducts User wallet, credits Agent wallet atomically |
| View Own Agent Transactions| `AGENT_VIEW_OWN_TRANSACTIONS`| ❌ | ✅ | ❌ | Only records where agent wallet is source or destination |
| **User Operations** | | | | | |
| View Own Profile | `USER_VIEW_PROFILE` | ❌ | ❌ | ✅ | Self only |
| View Own Balance | `USER_VIEW_BALANCE` | ❌ | ❌ | ✅ | Authoritative backend query for user's own wallet |
| View Own Transactions | `USER_VIEW_TRANSACTIONS` | ❌ | ❌ | ✅ | Self only (`wallet_id == user.wallet_id`) |
| Participate in Predictions | `USER_USE_GAME` | ❌ | ❌ | ✅ | Virtual coin lock; no payout without settled outcome |
| Change Own Password | `USER_CHANGE_PASSWORD` | ❌ | ❌ | ✅ | Requires current password validation |

---

## D. AUTHENTICATION FLOW

```
User / Agent / Admin Client                API Gateway / Auth Controller               PostgreSQL Database
             |                                           |                                      |
             | 1. POST /api/v1/auth/login                |                                      |
             |    { username/id, password }              |                                      |
             |------------------------------------------>|                                      |
             |                                           | 2. Fetch actor by ID/username across |
             |                                           |    hierarchical accounts (Admins,    |
             |                                           |    Agents, Users)                    |
             |                                           |------------------------------------->|
             |                                           |<-------------------------------------|
             |                                           |    Return record + password_hash     |
             |                                           | 3. If account not found:             |
             |                                           |    Record LOGIN_FAILED audit         |
             |                                           |    Return 401 Unauthorized (constant time)
             |                                           | 4. Verify password with Argon2id     |
             |                                           |    If mismatch: Log audit, return 401|
             |                                           | 5. Validate status == 'ACTIVE'       |
             |                                           |    If SUSPENDED/DISABLED: return 403 |
             |                                           | 6. Create Session Record             |
             |                                           |------------------------------------->|
             |                                           | 7. Mint Short-Lived Access JWT       |
             |                                           |    (Role + Permissions + Account ID) |
             |                                           | 8. Append AUDIT_LOG (LOGIN_SUCCESS)  |
             | 9. 200 OK                                 |                                      |
             |    { token, refreshToken, userDetails }   |                                      |
             |<------------------------------------------|                                      |
```

---

## E. AGENT CREATION FLOW

```
Admin Client                            Admin Controller                        Database (Transaction)
     |                                          |                                         |
     | 1. POST /api/v1/admin/agents             |                                         |
     |    Headers: Authorization Bearer (Admin) |                                         |
     |    { agentId, agentName, tempPassword,   |                                         |
     |      initialAllocation, notes }          |                                         |
     |----------------------------------------->|                                         |
     |                                          | 2. Enforce ADMIN_MANAGE_AGENTS          |
     |                                          | 3. Validate input format & unique ID   |
     |                                          | 4. Hash tempPassword using Argon2id    |
     |                                          | 5. BEGIN TRANSACTION (Serializable)     |
     |                                          |---------------------------------------->|
     |                                          | 6. INSERT INTO agents (admin_id, ...)   |
     |                                          | 7. INSERT INTO wallets (owner=agent, 0) |
     |                                          | 8. If initialAllocation > 0:            |
     |                                          |    Verify admin wallet balance >= alloc |
     |                                          |    UPDATE admin wallet balance -= alloc |
     |                                          |    UPDATE agent wallet balance += alloc |
     |                                          |    INSERT INTO wallet_transactions      |
     |                                          | 9. INSERT INTO audit_logs (AGENT_CREATE)|
     |                                          | 10. COMMIT TRANSACTION                  |
     |                                          |<----------------------------------------|
     | 11. 201 Created                          |                                         |
     |     { agentId, agentName, status,        |                                         |
     |       allocatedCoins, createdAt }        |                                         |
     |<-----------------------------------------|                                         |
```

---

## F. USER CREATION FLOW

```
Agent Client                            Agent Controller                        Database (Transaction)
     |                                          |                                         |
     | 1. POST /api/v1/agent/users              |                                         |
     |    Headers: Authorization Bearer (Agent) |                                         |
     |    { userId, displayName, tempPassword,  |                                         |
     |      initialAllocation, notes }          |                                         |
     |----------------------------------------->|                                         |
     |                                          | 2. Enforce AGENT_CREATE_USER            |
     |                                          | 3. Validate userId format and uniqueness|
     |                                          | 4. Hash tempPassword using Argon2id    |
     |                                          | 5. BEGIN TRANSACTION                    |
     |                                          |---------------------------------------->|
     |                                          | 6. INSERT INTO users (agent_id = auth)  |
     |                                          | 7. INSERT INTO wallets (owner=user, 0)  |
     |                                          | 8. If initialAllocation > 0:            |
     |                                          |    Verify agent wallet balance >= alloc |
     |                                          |    UPDATE agent wallet balance -= alloc |
     |                                          |    UPDATE user wallet balance += alloc  |
     |                                          |    INSERT INTO wallet_transactions      |
     |                                          | 9. INSERT INTO audit_logs (USER_CREATE) |
     |                                          | 10. COMMIT TRANSACTION                  |
     |                                          |<----------------------------------------|
     | 11. 201 Created                          |                                         |
     |     { userId, displayName, status,       |                                         |
     |       allocatedCoins, createdAt }        |                                         |
     |<-----------------------------------------|                                         |
```

---

## G. COIN TRANSACTION FLOW (IDEMPOTENT & ATOMIC)

```
Actor (Admin or Agent)                     Ledger Controller                    DB / Redis Lock Manager
     |                                             |                                       |
     | 1. POST /api/v1/wallets/transfer            |                                       |
     |    Headers: X-Idempotency-Key: <UUID>       |                                       |
     |    { destinationWalletId, amount, reason }  |                                       |
     |-------------------------------------------->|                                       |
     |                                             | 2. Check Idempotency Key in Redis     |
     |                                             |-------------------------------------->|
     |                                             |    If key exists & completed:         |
     |                                             |    Return cached response immediately |
     |                                             |    If key is currently in progress:   |
     |                                             |    Return 409 Conflict (Concurrent)   |
     |                                             | 3. Set Redis Lock (TTL 30s)           |
     |                                             | 4. BEGIN TRANSACTION                  |
     |                                             |-------------------------------------->|
     |                                             | 5. SELECT source & dest wallets       |
     |                                             |    FOR UPDATE (Row-level lock ordered |
     |                                             |    by wallet UUID to prevent deadlocks|
     |                                             | 6. Assert source.balance >= amount    |
     |                                             | 7. Update balances:                   |
     |                                             |    source.balance -= amount, ver += 1 |
     |                                             |    dest.balance += amount, ver += 1   |
     |                                             | 8. INSERT INTO wallet_transactions    |
     |                                             | 9. INSERT INTO audit_logs             |
     |                                             | 10. COMMIT TRANSACTION                |
     |                                             |<--------------------------------------|
     |                                             | 11. Cache Result in Redis (Idempotent)|
     | 12. 200 OK                                  |                                       |
     |     { txId, status: "SUCCESS", ... }        |                                       |
     |<--------------------------------------------|                                       |
```

---

## H. API SPECIFICATION (KEY ENDPOINTS)

### 1. Authentication
- `POST /api/v1/auth/login`
  - Body: `{ "username": "string", "password": "string" }`
  - Response 200: `{ "token": "string", "refreshToken": "string", "actor": { "id": "uuid", "role": "ADMIN|AGENT|USER", "status": "ACTIVE", "mustChangePassword": false } }`
- `POST /api/v1/auth/logout`
  - Headers: `Authorization: Bearer <token>`
  - Response 200: `{ "message": "Session invalidated" }`

### 2. Admin Operations
- `POST /api/v1/admin/agents`
  - Headers: `Authorization: Bearer <token>`, `X-Idempotency-Key: <uuid>`
  - Body: `{ "agentId": "agent01", "agentName": "North Division", "temporaryPassword": "string", "initialCoinAllocation": 5000, "notes": "string" }`
- `GET /api/v1/admin/agents?search=...&page=1&limit=20`
- `GET /api/v1/admin/agents/{id}`
- `POST /api/v1/admin/agents/{id}/suspend`
- `POST /api/v1/admin/agents/{id}/activate`
- `POST /api/v1/admin/agents/{id}/reset-password`
  - Body: `{ "newTemporaryPassword": "string" }`
- `GET /api/v1/admin/reports/system-summary`
- `GET /api/v1/admin/audit-logs`

### 3. Agent Operations
- `POST /api/v1/agent/users`
  - Headers: `Authorization: Bearer <token>`, `X-Idempotency-Key: <uuid>`
  - Body: `{ "userId": "usr_100", "displayName": "Alpha Player", "temporaryPassword": "string", "initialCoinAllocation": 500, "notes": "string" }`
- `GET /api/v1/agent/users?search=...&page=1&limit=20`
- `GET /api/v1/agent/users/{id}`
- `POST /api/v1/agent/users/{id}/suspend`
- `POST /api/v1/agent/users/{id}/activate`
- `POST /api/v1/agent/users/{id}/reset-password`

### 4. Coin Ledger
- `POST /api/v1/wallets/transfer`
  - Headers: `Authorization: Bearer <token>`, `X-Idempotency-Key: <uuid>`
  - Body: `{ "destinationWalletId": "uuid", "amount": 1000, "reason": "Monthly gameplay allocation" }`
- `POST /api/v1/wallets/deduct`
  - Headers: `Authorization: Bearer <token>`, `X-Idempotency-Key: <uuid>`
  - Body: `{ "targetWalletId": "uuid", "amount": 250, "reason": "Administrative adjustment" }`
- `GET /api/v1/wallets/my-wallet`
- `GET /api/v1/transactions?walletId=...&page=1&limit=30`

---

## I. THREAT MODEL (STRIDE)

| Threat Category | Attack Vector | Mitigation Strategy |
|---|---|---|
| **S**poofing | Attacker impersonates Admin or Agent using forged tokens or stolen IDs | Strong password hashing (Argon2id), short-lived signed JWTs, IP/User-Agent binding on refresh tokens, server-side session revocation blocklist. |
| **T**ampering | Client tampers with HTTP request to alter target role, wallet balance, or deduct amounts | Zero-Trust architecture. Client never provides role or balance; server retrieves state solely from DB. Balance checks enforced via atomic DB constraints. |
| **R**epudiation | Actor denies performing a critical coin allocation or account suspension | Append-only, tamper-evident audit log table capturing `actor_id`, `actor_role`, `request_id`, `before_state`, `after_state`, and cryptographic checksums. |
| **I**nformation Disclosure | Extraction of passwords, tokens, or other users' profiles via API leaks or APK decompilation | Passwords never logged or stored plaintext. Zero hardcoded keys in APK. Strict object-level authorization (IDOR protection) validating parent-child hierarchy. |
| **D**enial of Service | Rapid-fire coin transfer requests or credential stuffing brute force | Rate limiting via Redis Token Bucket, DB connection pooling with HikariCP, idempotent transaction locks preventing duplicate in-flight processing. |
| **E**levation of Privilege | User crafts API request to hit `/admin/agents` or an Agent attempts to manage another Agent's users | RBAC middleware checks JWT permissions on every endpoint. Hierarchical ownership checks (`user.agent_id == auth.agent_id`) enforced in service layer. |

---

## J. ANDROID PROJECT STRUCTURE

```
app/src/main/java/com/example/
├── MahakalApplication.kt
├── MainActivity.kt
├── core/
│   ├── network/
│   │   ├── ApiClient.kt
│   │   ├── AuthInterceptor.kt
│   │   ├── IdempotencyInterceptor.kt
│   │   └── NetworkResult.kt
│   ├── security/
│   │   ├── SecureTokenStorage.kt
│   │   └── BiometricAuthManager.kt
│   └── database/
│       ├── MahakalDatabase.kt
│       ├── entity/
│       │   ├── CachedWalletEntity.kt
│       │   └── CachedTransactionEntity.kt
│       └── dao/
│           ├── WalletDao.kt
│           └── TransactionDao.kt
├── domain/
│   ├── model/
│   │   ├── AccountRole.kt
│   │   ├── AuthSession.kt
│   │   ├── VirtualWallet.kt
│   │   └── LedgerTransaction.kt
│   └── repository/
│       ├── AuthRepository.kt
│       ├── AdminRepository.kt
│       ├── AgentRepository.kt
│       └── WalletRepository.kt
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── admin/
│   │   ├── AdminDashboardScreen.kt
│   │   ├── AgentManagementScreen.kt
│   │   ├── CreateAgentDialog.kt
│   │   ├── SystemAuditScreen.kt
│   │   └── AdminViewModel.kt
│   ├── agent/
│   │   ├── AgentDashboardScreen.kt
│   │   ├── UserManagementScreen.kt
│   │   ├── CreateUserDialog.kt
│   │   └── AgentViewModel.kt
│   ├── user/
│   │   ├── UserHomeScreen.kt
│   │   ├── UserWalletScreen.kt
│   │   ├── GamePredictionsScreen.kt
│   │   └── UserViewModel.kt
│   ├── components/
│   │   ├── MahakalAppBar.kt
│   │   ├── CoinBalanceCard.kt
│   │   └── TransactionListItem.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
```

---

## K. BACKEND PROJECT STRUCTURE

```
backend/
├── cmd/
│   └── server/
│       └── main.go (or Application.kt / server.ts)
├── config/
│   ├── DatabaseConfig.kt
│   └── SecurityConfig.kt
├── internal/
│   ├── auth/
│   │   ├── AuthController.kt
│   │   ├── AuthService.kt
│   │   └── TokenProvider.kt
│   ├── rbac/
│   │   ├── Permissions.kt
│   │   └── HierarchyGuard.kt
│   ├── admin/
│   │   ├── AdminController.kt
│   │   └── AdminService.kt
│   ├── agent/
│   │   ├── AgentController.kt
│   │   └── AgentService.kt
│   ├── ledger/
│   │   ├── WalletController.kt
│   │   ├── LedgerService.kt
│   │   └── IdempotencyManager.kt
│   ├── audit/
│   │   └── AuditLogger.kt
│   └── repository/
│       ├── AdminRepository.kt
│       ├── AgentRepository.kt
│       ├── UserRepository.kt
│       ├── WalletRepository.kt
│       └── TransactionRepository.kt
└── migrations/
    ├── 001_initial_schema.sql
    └── 002_indexes_and_constraints.sql
```

---

## L. PRODUCTION DEPLOYMENT ARCHITECTURE

```
[ DNS / Cloudflare WAF ]
         | (DDoS Protection, SSL Offload, GeoIP filter)
         v
[ Google Cloud Load Balancer / Ingress ]
         | (TLS 1.3, Header inspection)
         v
[ Google Cloud Run / GKE Private Cluster ]
    ├── Deployment: mahakal-backend (Replicas: 3-10 Auto-scaled)
    ├── Managed Identities (Workload Identity)
    └── Zero public IP exposure
         |
         +-----> Cloud KMS (Secrets & Encryption Keys)
         |
         +-----> Redis Memorystore (Idempotency Locks & Session Invalidation)
         |
         +-----> Cloud SQL PostgreSQL 16 (HA with Multi-AZ Standby)
                     - Disk Encryption: Customer-Managed Encryption Keys (CMEK)
                     - Automated Daily Backups + 7-Day Point-in-Time Recovery
```

---

## M. TESTING STRATEGY

1. **Unit & Isolation Testing**:
   - Password hashing verification (Argon2id resistance to timing attacks).
   - Invariant validation: Balance can never be negative under any arithmetic operations.
   - RBAC rules: Matrix verification of every `(Role, Permission, Resource)` tuple.
2. **Concurrency & Ledger ACID Tests**:
   - Double-spending simulation: 20 simultaneous threads attempting to deduct 800 coins from a 1,000 balance; exactly 1 succeeds and 19 fail with insufficient funds.
   - Idempotency replay testing: Replaying the identical transaction request 5 times results in 1 database execution and 5 identical idempotent responses.
3. **Android Client Verification**:
   - Robolectric tests for Critical User Journeys (Login, Role Redirection, Agent Creation, Coin Allocation).
   - Test-tag coverage on interactive controls (`Modifier.testTag`).
   - MockWebServer network failure simulation (offline handling, 401 token expiry automatic redirect to login).
