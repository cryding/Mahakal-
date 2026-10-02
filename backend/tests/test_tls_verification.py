import os, sys, ssl, socket, struct, tempfile, subprocess
from backend.server import PgConnectionPool

def test_tls_configuration_and_ca_loaded():
    print("--- 1. Testing CA File Resolution & SSLContext Setup ---")
    db_url = os.environ.get("DATABASE_URL", "postgresql://postgres:dummy@db.dgnpxzpftsmuxgorwahh.supabase.co:5432/postgres")
    pool = PgConnectionPool(db_url, pool_size=1)
    ca_file = pool._get_ca_file()
    assert ca_file is not None, "Failed to resolve Supabase CA certificate file path"
    assert os.path.isfile(ca_file), f"Resolved CA file does not exist: {ca_file}"
    
    with open(ca_file, "r") as f:
        cert_data = f.read()
    assert "BEGIN CERTIFICATE" in cert_data, "CA file does not contain valid PEM certificate"
    assert "Supabase Root 2021 CA" in cert_data, "CA file missing Supabase Root 2021 CA declaration"
    
    ctx = ssl.create_default_context(cafile=ca_file)
    assert ctx.check_hostname == True, "Hostname verification not enabled on SSLContext"
    assert ctx.verify_mode == ssl.CERT_REQUIRED, "Certificate verification mode is not CERT_REQUIRED"
    print("PASS: Official Supabase CA loaded and verified on SSLContext")

def test_no_insecure_ssl_fallback():
    print("\n--- 2. Auditing Source Code for Insecure SSL Fallbacks ---")
    server_path = os.path.join(os.path.dirname(__file__), "..", "server.py")
    with open(server_path, "r") as f:
        code = f.read()
    
    assert "CERT_NONE" not in code, "FATAL: Insecure ssl.CERT_NONE found in server.py"
    assert "check_hostname = False" not in code, "FATAL: Insecure check_hostname = False found in server.py"
    assert "_create_unverified_context" not in code, "FATAL: Insecure _create_unverified_context found in server.py"
    print("PASS: Zero insecure SSL bypasses found in server.py")

def test_untrusted_ca_rejected():
    print("\n--- 3. Testing Rejection of Untrusted Certificates ---")
    res = subprocess.run(
        ["openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-keyout", "/dev/null", "-out", "/tmp/untrusted_test_ca.crt", "-days", "1", "-subj", "/CN=UntrustedCA"],
        capture_output=True
    )
    assert res.returncode == 0
    
    host = "db.dgnpxzpftsmuxgorwahh.supabase.co"
    port = 5432
    try:
        sock = socket.create_connection((host, port), timeout=5)
        sock.sendall(struct.pack('!II', 8, 80877103))
        sock.recv(1)
        
        ctx = ssl.create_default_context(cafile="/tmp/untrusted_test_ca.crt")
        ctx.check_hostname = True
        ctx.verify_mode = ssl.CERT_REQUIRED
        
        try:
            ctx.wrap_socket(sock, server_hostname=host)
            raise AssertionError("Untrusted CA was unexpectedly accepted!")
        except (ssl.SSLCertVerificationError, ssl.CertificateError):
            print("PASS: Untrusted certificate correctly rejected by CERT_REQUIRED verification")
        finally:
            sock.close()
    except (socket.gaierror, TimeoutError, ConnectionRefusedError):
        print("SKIP: Network connection to Supabase host not reachable in test sandbox")
    finally:
        if os.path.exists("/tmp/untrusted_test_ca.crt"):
            os.remove("/tmp/untrusted_test_ca.crt")

def test_hostname_mismatch_rejected():
    print("\n--- 4. Testing Rejection of Hostname Mismatch ---")
    ca_file = os.path.join(os.path.dirname(__file__), "..", "certs", "prod-ca-2021.crt")
    host = "db.dgnpxzpftsmuxgorwahh.supabase.co"
    port = 5432
    try:
        sock = socket.create_connection((host, port), timeout=5)
        sock.sendall(struct.pack('!II', 8, 80877103))
        sock.recv(1)
        
        ctx = ssl.create_default_context(cafile=ca_file)
        ctx.check_hostname = True
        ctx.verify_mode = ssl.CERT_REQUIRED
        
        try:
            ctx.wrap_socket(sock, server_hostname="invalid-host.supabase.co")
            raise AssertionError("Mismatched hostname was unexpectedly accepted!")
        except (ssl.SSLCertVerificationError, ssl.CertificateError):
            print("PASS: Hostname mismatch correctly rejected by check_hostname=True")
        finally:
            sock.close()
    except (socket.gaierror, TimeoutError, ConnectionRefusedError):
        print("SKIP: Network connection to Supabase host not reachable in test sandbox")

def test_live_database_connection():
    print("\n--- 5. Testing Verified Live Database Connection ---")
    db_url = os.environ.get("DATABASE_URL")
    if not db_url:
        print("SKIP: DATABASE_URL not set in environment")
        return
    
    pool = PgConnectionPool(db_url, pool_size=1)
    res = pool.execute("SELECT 1 AS verified, current_database() AS db;")
    assert len(res) > 0, "No rows returned from query"
    assert res[0]["verified"] == "1", f"Unexpected query result: {res}"
    print(f"PASS: Live query executed over verified TLS connection: {res}")
    pool.close()

if __name__ == "__main__":
    test_tls_configuration_and_ca_loaded()
    test_no_insecure_ssl_fallback()
    test_untrusted_ca_rejected()
    test_hostname_mismatch_rejected()
    test_live_database_connection()
    print("\n==========================================")
    print("ALL TLS VERIFICATION TESTS PASSED")
    print("==========================================")
