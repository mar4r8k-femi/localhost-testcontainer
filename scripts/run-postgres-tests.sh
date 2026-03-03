#!/usr/bin/env bash
# run-postgres-tests.sh — Run only the Postgres integration tests.
#
# Optional env vars:
#   (none — Postgres image is pinned to postgres:16-alpine in the test class)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

bash "$SCRIPT_DIR/check-prereqs.sh"
echo ""

echo "=== Running Postgres Integration Tests ==="
echo "  Image  : postgres:16-alpine"
echo ""

cd "$PROJECT_DIR"
mvn test -B -Dtest="PostgresContainerTest" 2>&1 | tee target/postgres-test-output.log

echo ""
echo "=== Postgres Test Summary ==="
grep -E "Tests run:|BUILD" target/postgres-test-output.log | tail -5
