#!/usr/bin/env bash
# run-postgres-tests.sh — Run only the Postgres integration tests.
#
# Each test method gets its own fresh Postgres container with fixture data
# re-seeded via withInitScript (per-method lifecycle).
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
gradle test --tests "com.example.postgres.PostgresContainerTest.*" 2>&1 | tee build/postgres-test-output.log

echo ""
echo "=== Postgres Test Summary ==="
grep -E "tests|BUILD" build/postgres-test-output.log | tail -5
