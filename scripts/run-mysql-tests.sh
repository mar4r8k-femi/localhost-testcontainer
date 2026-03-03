#!/usr/bin/env bash
# run-mysql-tests.sh — Run only the MySQL integration tests.
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
mvn test -B -Dtest="MySqlContainerTest" 2>&1 | tee target/mysql-test-output.log

echo ""
echo "=== MySQL Test Summary ==="
grep -E "Tests run:|BUILD" target/mysql-test-output.log | tail -5
