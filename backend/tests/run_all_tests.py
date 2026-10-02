import subprocess, time, sys, os, signal

print("--- Starting Backend Server on port 8899 for Validation ---")
env = os.environ.copy()
env["PORT"] = "8899"
env["PYTHONPATH"] = "."
server_proc = subprocess.Popen([sys.executable, "backend/server.py"], env=env)

try:
    time.sleep(2.5)
    print("\n--- Running Test Suite 1: API Endpoints ---")
    ret1 = subprocess.call([sys.executable, "backend/tests/test_api_endpoints.py"], env=env)
    if ret1 != 0:
        raise RuntimeError(f"test_api_endpoints.py failed with exit code {ret1}")

    print("\n--- Running Test Suite 2: RBAC and Ledger ---")
    ret2 = subprocess.call([sys.executable, "backend/tests/test_rbac_and_ledger.py"], env=env)
    if ret2 != 0:
        raise RuntimeError(f"test_rbac_and_ledger.py failed with exit code {ret2}")

    print("\n--- Running Test Suite 3: TLS Certificate Verification ---")
    ret3 = subprocess.call([sys.executable, "backend/tests/test_tls_verification.py"], env=env)
    if ret3 != 0:
        raise RuntimeError(f"test_tls_verification.py failed with exit code {ret3}")

    print("\n==========================================")
    print("ALL BACKEND TEST SUITES PASSED SUCCESSFULLY")
    print("==========================================")

finally:
    print("\n--- Terminating Test Server ---")
    server_proc.terminate()
    try:
        server_proc.wait(timeout=5)
    except subprocess.TimeoutExpired:
        server_proc.kill()
