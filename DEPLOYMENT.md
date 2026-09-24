# MAHAKAL Production Infrastructure & Deployment Manual

**Application**: MAHAKAL Enterprise Virtual Coin Management & Prediction Platform  
**Target Architecture**: Android Modern Client + Microservices / In-Memory Server Router with Room/PostgreSQL Storage Engine  
**Classification**: STRICTLY NON-MONETARY ENTERPRISE GAMING INFRASTRUCTURE  

---

## 1. Non-Monetary Regulatory & Compliance Mandate
MAHAKAL is designed, built, and operated strictly as a non-monetary entertainment and simulation platform.
1. **Virtual Coins Only**: All points, coins, and balances represent non-redeemable virtual credits.
2. **Zero Fiat Inflow/Outflow**: No payment gateway (Razorpay, Stripe, Paytm, UPI, wire, cards) is integrated.
3. **No Cash-Out or Conversion**: There is no conversion rate between virtual coins and INR or any foreign currency.
4. **No Real-Money Wagering**: Any attempt to convert coin transactions into real-world monetary consideration is strictly prohibited and audited.

---

## 2. Environment Strategy & Configuration Matrix

MAHAKAL isolates runtime environments using distinct network endpoints, database schemas, and configuration variables.

| Environment | Purpose | Database | Ingress Base URL | Diagnostic Logs |
| :--- | :--- | :--- | :--- | :--- |
| **DEVELOPMENT** | Local testing & feature development | `mahakal_server_ledger_dev.db` | `https://dev-api.mahakal.internal/v1` | Enabled (Verbose) |
| **STAGING** | QA, pre-release validation, load tests | `mahakal_server_ledger_staging.db` | `https://staging-api.mahakal.internal/v1` | Enabled (Sanitized) |
| **PRODUCTION** | Live production traffic | `mahakal_server_ledger.db` / HA Postgres | `https://api.mahakal.internal/v1` | Disabled (Errors only) |

### Environment Variables Specification (`.env`)
- `APP_ENV`: Environment identifier (`development`, `staging`, `production`).
- `API_BASE_URL`: Authoritative server ingress URL.
- `SESSION_SECRET`: Cryptographically random 256-bit key for session token signing.
- `TOKEN_SIGNING_SECRET`: HS256/RS256 JWT signing secret.
- `DATABASE_URL`: Connection string with credentials and pool parameters.
- `OUTBOX_POLL_INTERVAL_MS`: Asynchronous background worker interval (default: 10,000 ms).
- `OUTBOX_MAX_RETRIES`: Number of retry attempts prior to Dead-Letter status (default: 5).

---

## 3. Database Migration Strategy & Lifecycle

### Storage Engine Architecture
The persistence layer utilizes SQLite (via Room with Android/JVM support) and managed PostgreSQL for high-scale backend deployments.

### Migration Schema Version History
The system tracks sequential schema migrations without destructive data loss:
- **v1**: Accounts, Sessions, Virtual Wallets, Transactions, Audit Logs, Rate Limits.
- **v2**: Ledger journal entries with double-entry idempotency and balance constraints.
- **v3**: Agent-to-User hierarchical permissions, quotas, and parent-child linkage.
- **v4**: Predictive games, game options, entries, and immutable results.
- **v5**: Game processing state machine, idempotent settlement, and audit reconciliation.
- **v6**: Push device registration, persistent notifications, and user notification preferences.
- **v7**: Outbox event table for transactional reliable message publishing.
- **v8**: Security events, account lockout tracking, IP anomaly detection, and session revocation.

### Zero-Downtime Migration Policy
1. **Additive Schema Changes**: All new columns must be nullable or have default values.
2. **Never Drop in Migration**: Deprecated columns are phased out over two release cycles (mark unused -> omit in application -> drop in future major version).
3. **Pre-Deployment Migration Execution**: Migrations are verified against replica databases before applying to live clusters.

---

## 4. High Availability & Disaster Recovery (DR)

### Backup Frequency & Retention
- **Continuous Write-Ahead Logging (WAL)**: Point-in-time recovery (PITR) with RPO < 5 minutes.
- **Hourly Incremental Snapshots**: Stored in multi-region object storage with 7-day retention.
- **Daily Full Backups**: Encrypted with AES-256 and stored with 90-day retention.

### Disaster Recovery Runbook
1. **Detection**: Health check endpoint `/health` or monitoring alerts signal database outage.
2. **Traffic Diversion**: Cloudflare / AWS Route 53 switches traffic to standby region.
3. **Database Restore**:
   ```bash
   # Restore latest verified WAL snapshot
   pg_restore --clean --if-exists -d mahakal_ledger_prod /backups/mahakal_latest.dump
   ```
4. **Integrity Audit**: Execute ledger balance reconciliation script (`validate_ledger_invariants.sh`) to verify asset parity.
5. **Resume Services**: Confirm readiness endpoint `/readiness` returns status 200 before routing user traffic.

---

## 5. Security & Secret Management

- **Zero Secrets in Git**: `.env` and `*.keystore` files are strictly added to `.gitignore`.
- **In-Memory Injection**: Production credentials and signing keys are passed strictly via container environment variables or CI/CD secrets.
- **ProGuard / R8 Obfuscation**: Release builds apply code minification and rule-based identifier obfuscation.
- **Android Keystore**: Client tokens and refresh keys are stored using `EncryptedSharedPreferences` backed by hardware-backed MasterKeys (KeyStore AES-256-GCM).

---

## 6. Health & Readiness Monitoring

The service exposes standardized liveness and readiness endpoints:
- **Liveness Endpoint** (`GET /health`):
  Verifies process execution, database connection availability, and rate limiter status.
- **Readiness Endpoint** (`GET /readiness`):
  Verifies database connectivity, Outbox worker background processing, and core service readiness before routing inbound traffic.

---

## 7. Release Automation & Rollback Procedure

### Standard Release Workflow
1. Feature branches merge to `main` following peer review and successful CI execution (`ci.yml`).
2. Release tags (`vX.Y.Z`) trigger automated release builds with release keystore signing.
3. Staged canary rollouts: 10% -> 25% -> 50% -> 100% over 48 hours.

### Rollback Procedure
If error rates exceed 0.5% or critical security anomalies are detected:
1. Halt canary rollout in Google Play Console / server deployment pipeline.
2. Revert traffic to the previously verified stable artifact (`vX.Y.(Z-1)`).
3. Issue session invalidation (`handleRevokeAllAccountSessions`) if token compromise is suspected.
