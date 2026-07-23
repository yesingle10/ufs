#!/bin/bash
set -e

BASE="http://localhost:8080"
KEY="app_demo"
SECRET="secret_demo"
PASS=0
FAIL=0

hmac_sign() {
  local METHOD="$1" PATH_URL="$2" BODY="$3"
  local TS=$(date +%s)
  local NONCE=$(python -c "import uuid;print(uuid.uuid4())")
  local BH
  if [ -z "$BODY" ]; then
    BH=$(printf '' | openssl dgst -sha256 -hex | awk '{print $NF}')
  else
    BH=$(printf '%s' "$BODY" | openssl dgst -sha256 -hex | awk '{print $NF}')
  fi
  local CANONICAL=$(printf '%s\n%s\n%s\n%s\n%s' "$METHOD" "$PATH_URL" "$TS" "$NONCE" "$BH")
  local SIG=$(printf '%s' "$CANONICAL" | openssl dgst -sha256 -hmac "$SECRET" -hex | awk '{print $NF}')
  AUTH="UFS ${KEY}:${SIG}"
  TS_HEADER="$TS"
  NONCE_HEADER="$NONCE"
}

hmac_get() {
  local PATH_URL="$1"
  hmac_sign "GET" "$PATH_URL" ""
  curl -s -w "\n%{http_code}" "$BASE$PATH_URL" \
    -H "Authorization: $AUTH" \
    -H "X-Ufs-Timestamp: $TS_HEADER" \
    -H "X-Ufs-Nonce: $NONCE_HEADER"
}

hmac_post() {
  local PATH_URL="$1" BODY="$2"
  hmac_sign "POST" "$PATH_URL" "$BODY"
  curl -s -w "\n%{http_code}" -X POST "$BASE$PATH_URL" \
    -H "Authorization: $AUTH" \
    -H "X-Ufs-Timestamp: $TS_HEADER" \
    -H "X-Ufs-Nonce: $NONCE_HEADER" \
    -H "Content-Type: application/json" \
    -d "$BODY"
}

check() {
  local NAME="$1" EXPECTED="$2" ACTUAL="$3"
  if echo "$ACTUAL" | grep -q "$EXPECTED"; then
    echo "[PASS] $NAME"
    PASS=$((PASS+1))
  else
    echo "[FAIL] $NAME -> got: $(echo "$ACTUAL" | tail -1)"
    FAIL=$((FAIL+1))
  fi
}

echo "=========================================="
echo "UFS Core End-to-End Test (dev-local profile)"
echo "=========================================="
echo ""

# --- GET endpoints ---
echo "--- GET Endpoints ---"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/v1/health")
check "GET /v1/health (no auth)" "200" "$RESP"
echo "  Body: $(echo "$RESP" | head -1)"

RESP=$(hmac_get "/v1/buckets")
check "GET /v1/buckets (HMAC)" "200" "$RESP"
echo "  Body: $(echo "$RESP" | head -1 | head -c 120)..."

RESP=$(hmac_get "/v1/monitor")
check "GET /v1/monitor (HMAC)" "200" "$RESP"

RESP=$(hmac_get "/v1/policies")
check "GET /v1/policies (HMAC)" "200" "$RESP"

RESP=$(hmac_get "/v1/audit")
check "GET /v1/audit (HMAC)" "200" "$RESP"

# --- Error scenarios ---
echo ""
echo "--- Error Scenarios ---"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/v1/buckets")
check "GET /v1/buckets (no auth -> 401)" "401" "$RESP"

# --- POST endpoints (the critical tests) ---
echo ""
echo "--- POST Endpoints (body-caching fix verification) ---"

BODY='{"name":"test-bucket-e2e","vendor":"local","region":"us-east-1","acl":"private"}'
RESP=$(hmac_post "/v1/buckets" "$BODY")
check "POST /v1/buckets (create bucket)" "200" "$RESP"
echo "  Body: $(echo "$RESP" | head -1 | head -c 150)..."

BODY='{"bucket":"ufs-demo-avatar","objectKey":"test-file.txt","fileName":"test-file.txt","fileSize":1024,"bizType":"avatar","multipart":false,"contentType":"text/plain"}'
RESP=$(hmac_post "/v1/objects/presign" "$BODY")
check "POST /v1/objects/presign (simple presign)" "200" "$RESP"
echo "  Body: $(echo "$RESP" | head -1 | head -c 150)..."

BODY='{"bucket":"ufs-demo-avatar","objectKey":"big-file.dat","fileName":"big-file.dat","fileSize":10485760,"bizType":"avatar","multipart":true,"partSize":5242880,"contentType":"application/octet-stream"}'
RESP=$(hmac_post "/v1/objects/presign" "$BODY")
check "POST /v1/objects/presign (multipart presign)" "200" "$RESP"
echo "  Body: $(echo "$RESP" | head -1 | head -c 150)..."

BODY='{"bizType":"avatar","provider":"local","bucket":"ufs-demo-avatar","maxFileSize":10485760,"allowedTypes":["image/png","image/jpeg"],"lifecycleDays":30}'
RESP=$(hmac_post "/v1/policies" "$BODY")
check "POST /v1/policies (create policy)" "200" "$RESP"
echo "  Body: $(echo "$RESP" | head -1 | head -c 150)..."

# --- Summary ---
echo ""
echo "=========================================="
echo "Results: $PASS passed, $FAIL failed"
echo "=========================================="
