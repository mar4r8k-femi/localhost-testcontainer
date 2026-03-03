#!/usr/bin/env bash
# run-redis-tests.sh — Run only the Redis integration tests.
#
# Optional env vars:
#   (none — Redis image is pinned to redis:7-alpine in the test class)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

bash "$SCRIPT_DIR/check-prereqs.sh"
echo ""

echo "=== Running Redis Integration Tests ==="
echo "  Image  : redis:7-alpine"
echo ""

cd "$PROJECT_DIR"
mvn test -B -Dtest="RedisContainerTest" 2>&1 | tee target/redis-test-output.log

echo ""
echo "=== Redis Test Summary ==="
grep -E "Tests run:|BUILD" target/redis-test-output.log | tail -5
