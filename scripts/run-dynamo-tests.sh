#!/usr/bin/env bash
# run-dynamo-tests.sh — Run only the DynamoDB integration tests against LocalStack.
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

echo "=== Running DynamoDB Integration Tests ==="
echo "  Image  : $LOCALSTACK_IMAGE"
echo "  Region : $AWS_DEFAULT_REGION"
echo ""

cd "$PROJECT_DIR"
mvn test -B -Dtest="DynamoLocalStackTest" 2>&1 | tee target/dynamo-test-output.log

echo ""
echo "=== DynamoDB Test Summary ==="
grep -E "Tests run:|BUILD" target/dynamo-test-output.log | tail -5
