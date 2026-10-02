import os, urllib.request, json, time, uuid

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

print("--- 1. Testing Health Endpoint ---")
status, res = request("/health")
assert status == 200, f"Expected 200, got {status}"
assert res["status"] == "OK", "Health status not OK"
print("Health Check: PASS", res)

print("\n--- 2. Testing Readiness Endpoint ---")
status, res = request("/readiness")
assert status == 200, f"Expected 200, got {status}"
assert res["ready"] == True, "Readiness not true"
print("Readiness Check: PASS", res)

print("\n--- 3. Testing Unauthorized Access ---")
status, res = request("/v1/auth/me")
assert status == 401, f"Expected 401, got {status}"
print("Unauthorized Guard: PASS", res["errorCode"])

print("\n--- 4. Testing Admin Login ---")
admin_pwd = os.environ.get("BOOTSTRAP_ADMIN_PASSWORD", "")
status, res = request("/v1/auth/login", method="POST", data={"loginId": "admin", "password": admin_pwd})
assert status == 200, f"Expected 200, got {status}: {res}"
admin_token = res["data"]["accessToken"]
admin_id = res["data"]["user"]["id"]
print("Admin Login: PASS (Received Bearer token)")

print("\n--- 5. Testing Profile Resolution (/auth/me) ---")
status, res = request("/v1/auth/me", token=admin_token)
assert status == 200, f"Expected 200, got {status}"
assert res["data"]["role"] == "ADMIN", "Role not ADMIN"
print("Profile Resolution: PASS", res["data"])

print("\n--- 6. Testing Admin Wallet Balance ---")
status, res = request("/v1/wallets/me", token=admin_token)
assert status == 200, f"Expected 200, got {status}"
assert res["data"]["balance"] >= 900000, f"Admin wallet balance unexpected: {res['data']['balance']}"
print("Admin Wallet Query: PASS", res["data"])

print("\n--- 7. Testing Invalid Password Attack ---")
status, res = request("/v1/auth/login", method="POST", data={"loginId": "admin", "password": "WrongPassword!"})
assert status == 401, f"Expected 401, got {status}"
print("Invalid Password Rejection: PASS", res["errorCode"])

print("\nALL BACKEND API TESTS PASSED SUCCESSFULLY!")
