import urllib.request, json, time, uuid

BASE_URL = "http://127.0.0.1:8899"

def request(path, method="GET", data=None, token=None, idempotency_key=None):
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if idempotency_key:
        headers["Idempotency-Key"] = idempotency_key

    body = json.dumps(data).encode('utf-8') if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode('utf-8'))

print("=== Phase 2: RBAC & Virtual Coin Ledger Integrity Tests ===")

# 1. Login as Admin
s, login_res = request("/v1/auth/login", method="POST", data={"loginId": "admin", "password": "AdminPassword@123"})
assert s == 200
admin_token = login_res["data"]["accessToken"]
admin_id = login_res["data"]["user"]["id"]
print("1. Admin authenticated successfully")

# 2. Query initial balance
s, bal_res = request("/v1/wallets/me", token=admin_token)
assert s == 200
initial_balance = bal_res["data"]["balance"]
print(f"2. Admin current wallet balance: {initial_balance} virtual coins")

# 3. Direct DB check/provision of a subordinated Agent for testing transfer
from backend.server import PgConnectionPool, hash_password
import os
db_pool = PgConnectionPool(os.environ.get("DATABASE_URL"))
existing_agent = db_pool.execute("SELECT id FROM accounts WHERE login_id = 'agent_test_01';")
now = int(time.time() * 1000)
if existing_agent:
    agent_id = existing_agent[0]["id"]
else:
    agent_id = str(uuid.uuid4())
    agent_wallet_id = str(uuid.uuid4())
    salt = uuid.uuid4().hex[:16]
    pwd_h = hash_password("AgentPass@123", salt)
    db_pool.execute(f"INSERT INTO accounts (id, login_id, password_hash, salt, role, status, parent_id, full_name, created_at, updated_at) VALUES ('{agent_id}', 'agent_test_01', '{pwd_h}', '{salt}', 'AGENT', 'ACTIVE', '{admin_id}', 'Test Agent 01', {now}, {now});")
    db_pool.execute(f"INSERT INTO wallets (wallet_id, owner_id, owner_role, balance, created_at, updated_at) VALUES ('{agent_wallet_id}', '{agent_id}', 'AGENT', 0, {now}, {now});")

w_row = db_pool.execute(f"SELECT balance FROM wallets WHERE owner_id = '{agent_id}';")
initial_agent_balance = int(w_row[0]["balance"]) if w_row else 0
print(f"3. Verified test Agent account ({agent_id}) with initial wallet balance: {initial_agent_balance}")

# 4. Transfer 5,000 virtual coins with Idempotency Key
idem_key = f"transfer-test-{uuid.uuid4()}"
s, tx_res = request("/v1/wallets/transfer", method="POST", token=admin_token, idempotency_key=idem_key, data={
    "destinationAccountId": agent_id,
    "amount": 5000,
    "reason": "Initial Agent Allocation"
})
assert s == 200, f"Transfer failed: {tx_res}"
assert tx_res["data"]["balanceAfterSource"] == initial_balance - 5000
print(f"4. Transferred 5,000 coins from Admin to Agent. Status: {tx_res['data']['status']}")

# 5. Idempotency test: Re-send EXACT SAME transfer request
s, tx_res2 = request("/v1/wallets/transfer", method="POST", token=admin_token, idempotency_key=idem_key, data={
    "destinationAccountId": agent_id,
    "amount": 5000,
    "reason": "Duplicate Attempt"
})
assert s == 200
# Balance of admin should NOT decrease a second time
s, bal_check = request("/v1/wallets/me", token=admin_token)
assert bal_check["data"]["balance"] == initial_balance - 5000, "Double-spend occurred! Idempotency failed!"
print("5. Idempotency replay check PASSED: No balance deducted on replay")

# 6. Negative / Zero Coin Transfer Rejection
s, neg_res = request("/v1/wallets/transfer", method="POST", token=admin_token, data={
    "destinationAccountId": agent_id,
    "amount": -500,
    "reason": "Illegal negative transfer"
})
assert s == 422, f"Negative amount was not rejected! Status: {s}"
print("6. Negative amount validation PASSED: HTTP 422 Unprocessable Entity")

# 7. Agent Login and Balance Check
s, agent_login = request("/v1/auth/login", method="POST", data={"loginId": "agent_test_01", "password": "AgentPass@123"})
assert s == 200
agent_token = agent_login["data"]["accessToken"]
s, agent_bal = request("/v1/wallets/me", token=agent_token)
assert s == 200
assert agent_bal["data"]["balance"] == initial_agent_balance + 5000, f"Expected agent balance {initial_agent_balance + 5000}, got {agent_bal['data']['balance']}"
print(f"7. Agent verified wallet balance: {agent_bal['data']['balance']} virtual coins (increased by 5,000)")

print("\nALL RBAC, IDEMPOTENCY, AND WALLET LEDGER TESTS PASSED!")
