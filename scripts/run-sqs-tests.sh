#!/usr/bin/env bash
# run-sqs-tests.sh — Run only the SQS integration tests against LocalStack.
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

echo "=== Running SQS Integration Tests ==="
echo "  Image  : $LOCALSTACK_IMAGE"
echo "  Region : $AWS_DEFAULT_REGION"
echo ""

cd "$PROJECT_DIR"
mvn test -B -Dtest="SqsLocalStackTest" 2>&1 | tee target/sqs-test-output.log

echo ""
echo "=== SQS Test Summary ==="
grep -E "Tests run:|BUILD" target/sqs-test-output.log | tail -5
