-- MAHAKAL Production PostgreSQL Schema v1.0.0
-- STRICTLY NON-MONETARY ENTERPRISE VIRTUAL COIN INFRASTRUCTURE

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Schema migration history table
CREATE TABLE IF NOT EXISTS schema_migrations (
    version INTEGER PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    applied_at BIGINT NOT NULL
);

-- Accounts Table
CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(64) PRIMARY KEY,
    login_id VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL CHECK (role IN ('ADMIN', 'AGENT', 'USER')),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DISABLED')),
    parent_id VARCHAR(64) REFERENCES accounts(id) ON DELETE SET NULL,
    full_name VARCHAR(128) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    last_login_at BIGINT,
    password_changed_at BIGINT,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until BIGINT,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_accounts_role ON accounts(role);
CREATE INDEX IF NOT EXISTS idx_accounts_parent_id ON accounts(parent_id);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(status);
CREATE INDEX IF NOT EXISTS idx_accounts_created_at ON accounts(created_at);
CREATE INDEX IF NOT EXISTS idx_accounts_parent_status ON accounts(parent_id, status);

-- Sessions Table
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(64) PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    access_token VARCHAR(255) UNIQUE NOT NULL,
    refresh_token VARCHAR(255) UNIQUE NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    last_activity_at BIGINT NOT NULL,
    user_agent TEXT,
    ip_address VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_sessions_account_id ON sessions(account_id);
CREATE INDEX IF NOT EXISTS idx_sessions_access_token ON sessions(access_token);
CREATE INDEX IF NOT EXISTS idx_sessions_refresh_token ON sessions(refresh_token);
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON sessions(expires_at);

-- Virtual Coin Wallets Table
CREATE TABLE IF NOT EXISTS wallets (
    wallet_id VARCHAR(64) PRIMARY KEY,
    owner_id VARCHAR(64) UNIQUE NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    owner_role VARCHAR(32) NOT NULL CHECK (owner_role IN ('ADMIN', 'AGENT', 'USER')),
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    currency_type VARCHAR(32) NOT NULL DEFAULT 'VIRTUAL_COIN',
    version BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_wallets_owner_id ON wallets(owner_id);

-- Wallet Ledger Transactions Table
CREATE TABLE IF NOT EXISTS wallet_transactions (
    transaction_id VARCHAR(64) PRIMARY KEY,
    idempotency_key VARCHAR(128) UNIQUE NOT NULL,
    timestamp BIGINT NOT NULL,
    actor_id VARCHAR(64) NOT NULL REFERENCES accounts(id),
    actor_role VARCHAR(32) NOT NULL,
    source_wallet_id VARCHAR(64) REFERENCES wallets(wallet_id),
    destination_wallet_id VARCHAR(64) REFERENCES wallets(wallet_id),
    amount BIGINT NOT NULL CHECK (amount > 0),
    balance_before_source BIGINT,
    balance_after_source BIGINT,
    balance_before_destination BIGINT,
    balance_after_destination BIGINT,
    transaction_type VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    reference_id VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    metadata_json TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transactions_actor_id ON wallet_transactions(actor_id);
CREATE INDEX IF NOT EXISTS idx_transactions_source_wallet ON wallet_transactions(source_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_dest_wallet ON wallet_transactions(destination_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON wallet_transactions(timestamp);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON wallet_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_reference_id ON wallet_transactions(reference_id);

-- Games Table
CREATE TABLE IF NOT EXISTS games (
    game_id VARCHAR(64) PRIMARY KEY,
    game_type VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    start_time BIGINT NOT NULL,
    entry_deadline BIGINT NOT NULL,
    result_time BIGINT NOT NULL,
    min_coins BIGINT NOT NULL DEFAULT 10,
    max_coins BIGINT NOT NULL DEFAULT 10000,
    reward_multiplier DOUBLE PRECISION NOT NULL DEFAULT 2.0,
    created_by VARCHAR(64) NOT NULL REFERENCES accounts(id),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_games_status ON games(status);
CREATE INDEX IF NOT EXISTS idx_games_start_time ON games(start_time);
CREATE INDEX IF NOT EXISTS idx_games_entry_deadline ON games(entry_deadline);

-- Game Options Table
CREATE TABLE IF NOT EXISTS game_options (
    option_id VARCHAR(64) PRIMARY KEY,
    game_id VARCHAR(64) NOT NULL REFERENCES games(game_id) ON DELETE CASCADE,
    option_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT NOT NULL DEFAULT '{}',
    CONSTRAINT uq_game_option_code UNIQUE (game_id, option_code)
);

CREATE INDEX IF NOT EXISTS idx_game_options_game_id ON game_options(game_id);

-- Game Entries Table
CREATE TABLE IF NOT EXISTS game_entries (
    entry_id VARCHAR(64) PRIMARY KEY,
    game_id VARCHAR(64) NOT NULL REFERENCES games(game_id),
    user_id VARCHAR(64) NOT NULL REFERENCES accounts(id),
    selected_option_id VARCHAR(64) NOT NULL REFERENCES game_options(option_id),
    virtual_coin_amount BIGINT NOT NULL CHECK (virtual_coin_amount > 0),
    status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    idempotency_key VARCHAR(128) UNIQUE NOT NULL,
    deduction_transaction_id VARCHAR(64) NOT NULL,
    reward_transaction_id VARCHAR(64),
    reward_amount BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_game_entries_game_id ON game_entries(game_id);
CREATE INDEX IF NOT EXISTS idx_game_entries_user_id ON game_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_game_entries_game_user ON game_entries(game_id, user_id);

-- Game Results Table
CREATE TABLE IF NOT EXISTS game_results (
    result_id VARCHAR(64) PRIMARY KEY,
    game_id VARCHAR(64) UNIQUE NOT NULL REFERENCES games(game_id),
    winning_option_id VARCHAR(64) NOT NULL REFERENCES game_options(option_id),
    result_status VARCHAR(32) NOT NULL DEFAULT 'FINALIZED',
    finalized_by VARCHAR(64) NOT NULL REFERENCES accounts(id),
    finalized_at BIGINT NOT NULL,
    result_version BIGINT NOT NULL DEFAULT 1,
    metadata_json TEXT NOT NULL DEFAULT '{}'
);

-- Game Processings Table
CREATE TABLE IF NOT EXISTS game_processings (
    processing_id VARCHAR(64) PRIMARY KEY,
    game_id VARCHAR(64) UNIQUE NOT NULL REFERENCES games(game_id),
    status VARCHAR(32) NOT NULL,
    started_at BIGINT NOT NULL,
    completed_at BIGINT,
    attempt_count INTEGER NOT NULL DEFAULT 1,
    last_error_code VARCHAR(64),
    last_error_message TEXT,
    correlation_id VARCHAR(64),
    total_entries INTEGER NOT NULL DEFAULT 0,
    processed_entries INTEGER NOT NULL DEFAULT 0,
    failed_entries INTEGER NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL
);

-- Game Events Table
CREATE TABLE IF NOT EXISTS game_events (
    event_id VARCHAR(64) PRIMARY KEY,
    game_id VARCHAR(64) NOT NULL REFERENCES games(game_id),
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    timestamp BIGINT NOT NULL,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    correlation_id VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_game_events_game_id ON game_events(game_id);
CREATE INDEX IF NOT EXISTS idx_game_events_timestamp ON game_events(timestamp);

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    notification_id VARCHAR(64) PRIMARY KEY,
    recipient_id VARCHAR(64) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    recipient_role VARCHAR(32) NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    body TEXT NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'INFO',
    reference_type VARCHAR(64),
    reference_id VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'UNREAD',
    created_at BIGINT NOT NULL,
    read_at BIGINT,
    expires_at BIGINT,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    correlation_id VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created ON notifications(recipient_id, created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_status ON notifications(recipient_id, status);

-- Notification Preferences Table
CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    game_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    result_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    wallet_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    security_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    operational_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at BIGINT NOT NULL
);

-- Device Sessions Table
CREATE TABLE IF NOT EXISTS device_sessions (
    device_id VARCHAR(128) NOT NULL,
    user_id VARCHAR(64) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    push_token TEXT NOT NULL,
    platform VARCHAR(32) NOT NULL DEFAULT 'ANDROID',
    app_version VARCHAR(32) NOT NULL,
    last_seen_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    revoked_at BIGINT,
    PRIMARY KEY (device_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_device_sessions_user_id ON device_sessions(user_id);

-- Outbox Events Table
CREATE TABLE IF NOT EXISTS outbox_events (
    event_id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    available_at BIGINT NOT NULL,
    processed_at BIGINT,
    last_error_code VARCHAR(64),
    correlation_id VARCHAR(64),
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_available ON outbox_events(status, available_at);

-- Security Events Table
CREATE TABLE IF NOT EXISTS security_events (
    id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64),
    actor_role VARCHAR(32),
    target_id VARCHAR(64),
    target_type VARCHAR(64),
    ip_address VARCHAR(64),
    user_agent TEXT,
    description TEXT NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_security_events_type ON security_events(event_type);
CREATE INDEX IF NOT EXISTS idx_security_events_severity ON security_events(severity);
CREATE INDEX IF NOT EXISTS idx_security_events_actor ON security_events(actor_id);
CREATE INDEX IF NOT EXISTS idx_security_events_created ON security_events(created_at);

-- Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_id VARCHAR(64),
    target_type VARCHAR(64),
    request_id VARCHAR(64) NOT NULL,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    before_state TEXT,
    after_state TEXT,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_target_id ON audit_logs(target_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

-- Rate Limits Table
CREATE TABLE IF NOT EXISTS rate_limits (
    rate_key VARCHAR(128) PRIMARY KEY,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    window_start BIGINT NOT NULL,
    locked_until BIGINT
);

CREATE INDEX IF NOT EXISTS idx_rate_limits_locked ON rate_limits(locked_until);
