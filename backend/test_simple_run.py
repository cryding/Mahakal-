import os, sys, traceback
from test_migration_plan import MIGRATION_V1_SQL

print("Running test_simple_run...")
from backend.server import PgConnectionPool
db_url = os.environ.get('DATABASE_URL', '')
pool = PgConnectionPool(db_url)
print("Pool created, executing SELECT 1...")
res = pool.execute("SELECT 1 AS num;")
print("Result:", res)
