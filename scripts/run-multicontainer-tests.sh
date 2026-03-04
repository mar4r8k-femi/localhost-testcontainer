#!/usr/bin/env bash
# run-multicontainer-tests.sh — Run the multi-container integration tests.
#
# TwoContainerTest   — Postgres + Redis running simultaneously per test method.
# ThreeContainerTest — LocalStack (S3) + Postgres + Redis simultaneously.
#
# Optional env vars:
#   LOCALSTACK_IMAGE   Override the LocalStack Docker image (default: localstack/localstack:3.0)
#   AWS_DEFAULT_REGION Override the AWS region                (default: us-east-1)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

bash "$SCRIPT_DIR/check-prereqs.sh"
echo ""

export LOCALSTACK_IMAGE="${LOCALSTACK_IMAGE:-localstack/localstack:3.0}"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"

echo "=== Running Multi-Container Integration Tests ==="
echo "  TwoContainerTest   : Postgres + Redis"
echo "  ThreeContainerTest : LocalStack (S3) + Postgres + Redis"
echo "  LocalStack img     : $LOCALSTACK_IMAGE"
echo "  Region             : $AWS_DEFAULT_REGION"
echo ""

cd "$PROJECT_DIR"
gradle test --tests "com.example.multicontainer.*" 2>&1 | tee build/multicontainer-test-output.log

echo ""
echo "=== Multi-Container Test Summary ==="
grep -E "tests|BUILD" build/multicontainer-test-output.log | tail -5
