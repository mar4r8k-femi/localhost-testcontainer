#!/usr/bin/env bash
# run-mysql-tests.sh — Run only the MySQL integration tests.
#
# Each test method gets its own fresh MySQL container with fixture data
# re-seeded via withInitScript (per-method lifecycle).
#
# Optional env vars:
#   (none — MySQL image is pinned to mysql:8 in the test class)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

bash "$SCRIPT_DIR/check-prereqs.sh"
echo ""

echo "=== Running MySQL Integration Tests ==="
echo "  Image  : mysql:8"
echo ""

cd "$PROJECT_DIR"
gradle test --tests "com.example.mysql.MySqlContainerTest.*" 2>&1 | tee build/mysql-test-output.log

echo ""
echo "=== MySQL Test Summary ==="
grep -E "tests|BUILD" build/mysql-test-output.log | tail -5
