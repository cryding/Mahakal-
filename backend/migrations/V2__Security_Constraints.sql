-- MAHAKAL Production PostgreSQL Schema v2.0.0
-- Security, Idempotency, and Tenant-Isolation Hardening Constraints

-- Ensure fast and index-backed hierarchy lookups for Agent-User tenant isolation
CREATE INDEX IF NOT EXISTS idx_accounts_parent_role ON accounts(parent_id, role);

-- Ensure index-backed lookups on wallet transactions by actor and idempotency key
CREATE INDEX IF NOT EXISTS idx_transactions_actor_idempotency ON wallet_transactions(actor_id, idempotency_key);

-- Ensure index-backed lookups on game entries by game and selected option for settlement
CREATE INDEX IF NOT EXISTS idx_game_entries_game_opt_status ON game_entries(game_id, selected_option_id, status);
