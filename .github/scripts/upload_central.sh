#!/usr/bin/env bash
set -euo pipefail

# Upload a Central Publisher Portal bundle (USER_MANAGED) using curl.
# Usage:
#   upload_central.sh <path-to-bundle-zip>
#
# Env vars required:
#   CENTRAL_USERNAME  Your Central/OSSRH username
#   CENTRAL_PASSWORD  Your Central/OSSRH password
#
# Notes:
# - This script does NOT build anything; it only performs the HTTP upload.
# - The bundle can be prepared with:
#       ./gradlew prepareCentralBundle
#   which places the zip at build/central-bundle.zip

BUNDLE_PATH="${1:-}"
if [[ -z "$BUNDLE_PATH" ]]; then
  echo "Usage: $0 <path-to-bundle-zip>" >&2
  exit 2
fi

if [[ ! -f "$BUNDLE_PATH" ]]; then
  echo "Bundle file not found: $BUNDLE_PATH" >&2
  exit 2
fi

: "${CENTRAL_USERNAME:?CENTRAL_USERNAME env var required}"
: "${CENTRAL_PASSWORD:?CENTRAL_PASSWORD env var required}"

# Build the base64 token username:password with no newlines
if TOKEN=$(printf "%s:%s" "$CENTRAL_USERNAME" "$CENTRAL_PASSWORD" | base64 -w 0 2>/dev/null); then
  :
else
  TOKEN=$(printf "%s:%s" "$CENTRAL_USERNAME" "$CENTRAL_PASSWORD" | base64 | tr -d '\n')
fi

URL="https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED"

TMP_BODY="$(mktemp)"
HTTP_CODE=$(curl -sS -o "$TMP_BODY" -w "%{http_code}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "bundle=@${BUNDLE_PATH}" \
  "$URL")

if [[ "$HTTP_CODE" == "201" ]]; then
  DEPLOYMENT_ID=$(tr -d '\r\n' < "$TMP_BODY")
  echo "Upload successful. Deployment ID: ${DEPLOYMENT_ID}"
  rm -f "$TMP_BODY"
else
  echo "Upload failed with HTTP ${HTTP_CODE}" >&2
  echo "Response body:" >&2
  cat "$TMP_BODY" >&2
  rm -f "$TMP_BODY"
  exit 1
fi
