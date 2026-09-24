# MAHAKAL Release Notes

**Application**: MAHAKAL Enterprise Virtual Coin Management & Prediction Platform  
**Version**: 1.0.0 (Build 1)  
**Release Date**: 2026-09-22  
**Environment**: Production Ready (RC-1)  
**Database Schema Version**: Room Migration v8  
**Classification**: Strictly Non-Monetary Virtual Coin Platform  

---

## 1. Executive Summary
MAHAKAL v1.0.0 represents the final consolidated production release of the hierarchical virtual coin management, prediction gaming, and immutable audit platform. The release enforces strict role-based access control (Admin, Agent, User), double-entry ledger bookkeeping, server-authoritative balance calculation, transactional outbox notifications, and multi-tier security hardening.

---

## 2. Major Completed Capabilities

### 2.1 Role-Based Portals & Dashboards
- **Master Admin Operations**:
  - Global oversight across all agents, users, system games, and financial ledgers.
  - Agent lifecycle management: provisioning, credit limits, account status toggles, and forced password resets.
  - Comprehensive audit log inspection and automated reconciliation reports.
- **Agent Portal**:
  - Strict tenant boundary enforcement: agents can only view, manage, and transfer coins to users registered directly under their agent umbrella.
  - Real-time double-entry transfer and deduction modals with idempotency key deduplication.
  - User creation, password management, and unread notification alerts.
- **User Portal**:
  - Live prediction arena with prediction games (`MARKET_TREND`, `DIGIT_PREDICTION`), deadline timers, and dynamic odds multipliers.
  - Detailed game rules, options inspect dialog, and transparent entry receipts.
  - Results and history tracking (`WON`, `LOST`, `REFUNDED`) with authoritative settlement rewards.
  - Dedicated Profile and Security settings with verified session indicators.

### 2.2 Financial & Ledger Engine
- **Server-Authoritative Invariants**:
  - Balances are strictly computed and verified on the server; client-side balance mutations are strictly prohibited.
  - Double-entry ledger architecture ensuring `balance = ledger-consistent balance`.
  - Every transaction is recorded with balance-before and balance-after checkpoints, idempotency guarantees, and immutable transaction IDs.
  - Admin audit ledger reconciliation tools with automatic variance detection.

### 2.3 Game Engine & Settlement
- **Deterministic Lifecycle Machine**:
  - Strictly enforced status progression: `DRAFT` → `SCHEDULED` → `OPEN` → `CLOSED` → `RESULT_PENDING` → `RESULT_FINALIZED` (or `CANCELLED` / `ARCHIVED`).
  - Pre-deadline cutoffs rejecting late entries down to the millisecond.
  - Idempotent result processing with retry mechanisms, winner distribution, and automatic refunds upon cancellation.

### 2.4 Notifications & Outbox Worker
- **Guaranteed Event Delivery**:
  - Multi-channel notification pipeline (In-App notifications + persistent transactional outbox).
  - Background `OutboxWorker` processing batches with exponential backoff and dead-letter error handling.
  - Granular user preferences for game, result, wallet, security, and operational alerts.

### 2.5 Security Hardening & Zero-Trust Governance
- **Authentication & Sessions**:
  - Argon2id slow-hashing algorithm with unique cryptographically random salts for all credentials.
  - Rotating bearer tokens with token family reuse detection (automatic invalidation of all sessions upon compromised token replay).
  - Rate limiting and lockout protection against brute force and credential stuffing attacks.
  - IDOR (Insecure Direct Object Reference) prevention blocking unauthorized cross-account and cross-agent queries.
  - Android client secure storage backed by `EncryptedSharedPreferences` / Android KeyStore.

---

## 3. Strict Non-Monetary Compliance
In full adherence to regulatory policies, MAHAKAL strictly prohibits and does not implement:
1. **Zero Fiat Inflow/Outflow**: No cash deposits, bank accounts, UPI, debit/credit cards, or payment gateway integrations.
2. **Zero Cryptocurrency / Crypto-Asset Gateways**: No blockchain tokens or crypto transfers.
3. **No Cash Conversions or Withdrawals**: Virtual coins hold no cash equivalent and cannot be redeemed for fiat currency (INR, USD, etc.).
4. **No Real-Money Wagering**: Virtual coins are strictly for platform entertainment, skill prediction, and educational simulations.

---

## 4. Database & Infrastructure Status
- **Persistence Engine**: SQLite (via Room) with Room Migrations 1 through 8.
- **Migration Schema**:
  - v1: Core Accounts, Sessions, Wallets, Transactions, Audit Logs, Rate Limits.
  - v2: Notes and metadata schema extension.
  - v3: Status and timestamp indexing for high-speed queries.
  - v4: Scaled wallet journal indexes and idempotency constraints.
  - v5: Prediction games, options, user entries, results, and game events.
  - v6: Game asynchronous processing state machine and retry counters.
  - v7: Notifications, user notification preferences, outbox events, and device sessions.
  - v8: Security events, anomaly tracking, and IP audit records.
- **Disaster Recovery**:
  - Backup configuration documented in `DEPLOYMENT.md`.
  - Continuous WAL and periodic encrypted snapshots.
  - *Restore Verification Notice*: Formal automated disaster recovery restore run test in live production environment is pending scheduled cluster maintenance (**RESTORE TEST NOT VERIFIED** in live cloud cluster).

---

## 5. Known Limitations & Operational Guidelines
- **Offline Entry Submission**: Entries and coin transactions require an active network connection to prevent double-spending; offline caching is strictly read-only for previously loaded data.
- **Hardware Keystore Availability**: In local JVM test runners and unsupported emulators without Android KeyStore, secure storage falls back to sanitized in-memory key isolation. Hardware-backed secure enclave is fully engaged on physical Android devices.
- **Push Delivery Provider**: Transactional outbox handles in-app queues; live external FCM push dispatch requires provisioning `google-services.json` in production deployments.
