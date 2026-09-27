import os, re, socket, ssl, struct, base64, hashlib, hmac

db_url = os.environ.get('DATABASE_URL', '')
m = re.match(r'^(?:postgresql|postgres)://([^:]+):(.*)@([^:/]+)(?::(\d+))?/(.*)$', db_url)
user, pwd, host, port, db_part = m.groups()
port = int(port or 5432)
dbname = db_part.split('?')[0]

class CleanPgClient:
    def __init__(self, host, port, user, pwd, dbname):
        ais = socket.getaddrinfo(host, port, socket.AF_UNSPEC, socket.SOCK_STREAM)
        s = socket.socket(ais[0][0], ais[0][1], ais[0][2])
        s.settimeout(20.0)
        s.connect(ais[0][4])
        s.sendall(struct.pack('!II', 8, 80877103))
        s.recv(1)
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        self.sock = ctx.wrap_socket(s, server_hostname=host)
        params = b'user\x00' + user.encode('utf-8') + b'\x00database\x00' + dbname.encode('utf-8') + b'\x00\x00'
        self.sock.sendall(struct.pack('!II', 8 + len(params), 196608) + params)
        self.sock.recv(1)
        l = struct.unpack('!I', self.sock.recv(4))[0]
        self.sock.recv(l - 4)

        c_nonce = base64.b64encode(os.urandom(18)).decode('ascii')
        client_first_bare = f'n={user},r={c_nonce}'
        client_first = f'n,,{client_first_bare}'
        sasl_initial = b'SCRAM-SHA-256\x00' + struct.pack('!I', len(client_first)) + client_first.encode('ascii')
        self.sock.sendall(b'p' + struct.pack('!I', 4 + len(sasl_initial)) + sasl_initial)
        self.sock.recv(1)
        r_len = struct.unpack('!I', self.sock.recv(4))[0]
        self.sock.recv(4)
        server_first = self.sock.recv(r_len - 8).decode('ascii')
        sf_dict = dict(item.split('=', 1) for item in server_first.split(','))
        s_nonce = sf_dict['r']
        salt = base64.b64decode(sf_dict['s'])
        iterations = int(sf_dict['i'])

        def h(b): return hashlib.sha256(b).digest()
        def hmac_sha256(key, msg): return hmac.new(key, msg, hashlib.sha256).digest()
        def xor(b1, b2): return bytes(x ^ y for x, y in zip(b1, b2))

        salted_password = hashlib.pbkdf2_hmac('sha256', pwd.encode('utf-8'), salt, iterations, dklen=32)
        client_key = hmac_sha256(salted_password, b'Client Key')
        stored_key = h(client_key)
        client_final_without_proof = f'c=biws,r={s_nonce}'
        auth_message = f'{client_first_bare},{server_first},{client_final_without_proof}'.encode('ascii')
        client_signature = hmac_sha256(stored_key, auth_message)
        client_proof = xor(client_key, client_signature)
        client_final = f'{client_final_without_proof},p={base64.b64encode(client_proof).decode("ascii")}'

        self.sock.sendall(b'p' + struct.pack('!I', 4 + len(client_final)) + client_final.encode('ascii'))
        self.sock.recv(1)
        f_len = struct.unpack('!I', self.sock.recv(4))[0]
        self.sock.recv(f_len - 4)
        self.sock.recv(1)
        ok_len = struct.unpack('!I', self.sock.recv(4))[0]
        self.sock.recv(ok_len - 4)

        while True:
            t = self.sock.recv(1)
            if not t: break
            pl = struct.unpack('!I', self.sock.recv(4))[0]
            self.sock.recv(pl - 4)
            if t == b'Z': break

    def recv_exact(self, n):
        b = bytearray()
        while len(b) < n:
            chunk = self.sock.recv(n - len(b))
            if not chunk:
                raise ConnectionResetError("Connection closed while receiving")
            b.extend(chunk)
        return bytes(b)

    def execute(self, sql):
        q = sql.encode('utf-8') + b'\x00'
        self.sock.sendall(b'Q' + struct.pack('!I', 4 + len(q)) + q)
        rows = []
        col_names = []
        err = None
        while True:
            t = self.recv_exact(1)
            pl = struct.unpack('!I', self.recv_exact(4))[0]
            payload = self.recv_exact(pl - 4)
            if t == b'T':
                num_fields = struct.unpack('!H', payload[:2])[0]
                offset = 2
                col_names = []
                for _ in range(num_fields):
                    null_idx = payload.find(b'\x00', offset)
                    name = payload[offset:null_idx].decode('utf-8')
                    col_names.append(name)
                    offset = null_idx + 1 + 18
            elif t == b'D':
                num_fields = struct.unpack('!H', payload[:2])[0]
                offset = 2
                row = []
                for _ in range(num_fields):
                    flen = struct.unpack('!i', payload[offset:offset+4])[0]
                    offset += 4
                    if flen == -1:
                        row.append(None)
                    else:
                        row.append(payload[offset:offset+flen].decode('utf-8'))
                        offset += flen
                rows.append(row)
            elif t == b'E':
                err = payload.decode('utf-8', errors='ignore')
            elif t == b'Z':
                break
        if err:
            raise Exception(f'PostgreSQL query error: {err}')
        if col_names and rows:
            return [dict(zip(col_names, r)) for r in rows]
        return rows

client = CleanPgClient(host, port, user, pwd, dbname)
res = client.execute("SELECT count(*) as total_accounts FROM accounts;")
print("Execute success! Result:", res)
