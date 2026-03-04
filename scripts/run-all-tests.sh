#!/usr/bin/env bash
# run-all-tests.sh — Run the full integration test suite.
#
# Each test method gets its own fresh container (per-method lifecycle).
# Testcontainers starts the container before @BeforeEach and destroys it
# automatically after each test — no manual cleanup needed.
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

echo "=== Running All Integration Tests ==="
echo "  Services       : LocalStack (S3, SQS, DynamoDB), Redis, MySQL, Postgres"
echo "  Container scope: per test method (fresh container per @Test)"
echo "  LocalStack img : $LOCALSTACK_IMAGE"
echo "  Region         : $AWS_DEFAULT_REGION"
echo ""

cd "$PROJECT_DIR"
gradle test 2>&1 | tee build/test-output.log

echo ""
echo "=== Test Summary ==="
grep -E "tests|BUILD" build/test-output.log | tail -10
