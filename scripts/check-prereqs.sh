#!/usr/bin/env bash
# check-prereqs.sh — Verify all local dependencies are met before running tests.
set -euo pipefail

PASS="[OK]"
FAIL="[FAIL]"
errors=0

echo "=== LocalStack Testcontainers — Prerequisite Check ==="
echo ""

# ── Docker ────────────────────────────────────────────────────────────────────
if command -v docker &>/dev/null; then
    echo "$PASS docker found: $(docker --version)"
else
    echo "$FAIL docker not found — install Docker Desktop: https://www.docker.com/products/docker-desktop"
    errors=$((errors + 1))
fi

if docker info &>/dev/null 2>&1; then
    echo "$PASS Docker daemon is running"
else
    echo "$FAIL Docker daemon is not running — start Docker Desktop and try again"
    errors=$((errors + 1))
fi

# ── Java 17+ ─────────────────────────────────────────────────────────────────
java_ver_output=$(java -version 2>&1 || true)
if echo "$java_ver_output" | grep -q 'version "'; then
    java_version=$(echo "$java_ver_output" | awk -F'"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ -n "$java_version" ] && [ "$java_version" -ge 17 ]; then
        echo "$PASS java found: $(echo "$java_ver_output" | head -1)"
    else
        echo "$FAIL Java 17+ required, found version ${java_version:-unknown}"
        errors=$((errors + 1))
    fi
else
    echo "$FAIL java not found or not functional — install Java 17+ (e.g. brew install --cask temurin@17)"
    errors=$((errors + 1))
fi

# ── Maven ─────────────────────────────────────────────────────────────────────
if command -v mvn &>/dev/null; then
    echo "$PASS mvn found: $(mvn -version | head -1)"
else
    echo "$FAIL mvn not found — install Maven (e.g. brew install maven)"
    errors=$((errors + 1))
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
if [ "$errors" -eq 0 ]; then
    echo "All prerequisites met. You can run the tests."
    exit 0
else
    echo "$errors prerequisite(s) failed. Fix the issues above before running tests."
    exit 1
fi
