# MAHAKAL Role-Based Access Control (RBAC) & Hierarchy Foundation (Phase 2)

## 1. The Strict Three-Level Hierarchy
```
        ┌──────────────┐
        │    ADMIN     │  (Top-level system authority)
        └──────┬───────┘
               │
        ┌──────▼───────┐
        │    AGENT     │  (Subordinated regional distributor)
        └──────┬───────┘
               │
        ┌──────▼───────┐
        │     USER     │  (Subordinated player/participant)
        └──────────────┘
```

---

## 2. Permissions Breakdown

### ADMIN Role Permissions
- `ADMIN_MANAGE_AGENTS`: Provision and control Agent accounts.
- `ADMIN_MANAGE_USERS`: Global administrative oversight of User accounts.
- `ADMIN_MANAGE_AGENT_COINS`: Mint and distribute non-monetary virtual gameplay coins to Agents.
- `ADMIN_VIEW_TRANSACTIONS`: Full ledger visibility across all transactions.
- `ADMIN_VIEW_REPORTS`: System aggregate telemetry and accounting reports.
- `ADMIN_VIEW_AUDIT`: Immutable audit log examination.
- `ADMIN_MANAGE_SYSTEM`: Platform configuration and operational settings.

### AGENT Role Permissions
- `AGENT_CREATE_USER`: Provision User accounts assigned strictly under their Agent ID.
- `AGENT_MANAGE_OWN_USERS`: Manage status and lifecycle of directly subordinated users.
- `AGENT_MANAGE_OWN_USER_COINS`: Distribute virtual gameplay coins from Agent balance to subordinated Users.
- `AGENT_VIEW_OWN_TRANSACTIONS`: View ledger entries where Agent is sender or receiver.
- `AGENT_VIEW_OWN_USERS`: List and inspect Users assigned to this specific Agent.

### USER Role Permissions
- `USER_VIEW_PROFILE`: View own account profile and session status.
- `USER_VIEW_BALANCE`: View own non-monetary virtual coin wallet balance.
- `USER_VIEW_TRANSACTIONS`: View own coin activity ledger entries.
- `USER_USE_GAME`: Access gameplay and prediction system.
- `USER_CHANGE_PASSWORD`: Update own authentication credentials.

---

## 3. Hierarchical Ownership Validation Rules
- **Rule 1 (Admin Sovereignty)**: An Admin can manage and view any Agent or User across the entire system.
- **Rule 2 (Agent Boundary)**: An Agent can ONLY interact with Users where `user.parentId == authenticatedAgent.id`. Attempts to access another Agent's users fail with `403 Forbidden`.
- **Rule 3 (User Isolation)**: A User can ONLY access their own records (`resourceOwnerId == authenticatedUser.id`). Attempts to query another user's balance, transactions, or profile fail with `403 Forbidden`.
- **Rule 4 (No Client Trust)**: Roles and parent identifiers sent by client requests are ignored. The server derives the caller's role, permissions, and ID exclusively from the cryptographically verified session token.
