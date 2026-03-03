#!/usr/bin/env bash
# run-all-tests.sh — Run the full LocalStack integration test suite.
#
# Optional env vars:
#   LOCALSTACK_IMAGE   Override the LocalStack Docker image (default: localstack/localstack:3.0)
#   AWS_DEFAULT_REGION Override the AWS region                (default: us-east-1)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# ── Prereq check ──────────────────────────────────────────────────────────────
bash "$SCRIPT_DIR/check-prereqs.sh"
echo ""

# ── Defaults ──────────────────────────────────────────────────────────────────
export LOCALSTACK_IMAGE="${LOCALSTACK_IMAGE:-localstack/localstack:3.0}"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
# Disable Ryuk so it cannot reap the shared LocalStack container between test
# classes. Testcontainers' JVM shutdown hook handles cleanup on exit instead.
export TESTCONTAINERS_RYUK_DISABLED=true

echo "=== Running All LocalStack Integration Tests ==="
echo "  Image  : $LOCALSTACK_IMAGE"
echo "  Region : $AWS_DEFAULT_REGION"
echo ""

cd "$PROJECT_DIR"
mvn test -B 2>&1 | tee target/test-output.log

echo ""
echo "=== Test Summary ==="
grep -E "Tests run:|BUILD" target/test-output.log | tail -10
