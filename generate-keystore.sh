#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# generate-keystore.sh — Generate a release signing keystore for Pollen
#
# Run ONCE and keep the output secure. Back it up somewhere safe.
# You'll need it for every future release.
#
# Usage:
#   chmod +x generate-keystore.sh
#   ./generate-keystore.sh
#
# After running:
#   1. Copy the base64 output to GitHub secret: SIGNING_KEY_BASE64
#   2. Add the other secrets: SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD, SIGNING_STORE_PASSWORD
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

KEYSTORE_FILE="pollen-release.jks"
KEY_ALIAS="pollen-release"
VALIDITY_DAYS=10000   # ~27 years

echo "🌼 Pollen Release Keystore Generator"
echo "────────────────────────────────────"

if [ -f "$KEYSTORE_FILE" ]; then
  echo "⚠️  $KEYSTORE_FILE already exists. Delete it first to regenerate."
  exit 1
fi

# Prompt for passwords
read -sp "Enter keystore password: " STORE_PASS; echo
read -sp "Confirm keystore password: " STORE_PASS2; echo
if [ "$STORE_PASS" != "$STORE_PASS2" ]; then echo "Passwords don't match."; exit 1; fi

read -sp "Enter key password (or press Enter to use same as keystore): " KEY_PASS; echo
if [ -z "$KEY_PASS" ]; then KEY_PASS="$STORE_PASS"; fi

echo ""
echo "Generating keystore..."

keytool -genkey -v \
  -keystore "$KEYSTORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg EC \
  -keysize 256 \
  -validity "$VALIDITY_DAYS" \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=Pollen, OU=Android, O=stackbleed-ctrl, L=Nova Scotia, ST=NS, C=CA"

echo ""
echo "✅ Keystore generated: $KEYSTORE_FILE"
echo ""

# Print fingerprint
echo "📋 Certificate fingerprint (add to F-Droid metadata):"
keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$KEY_ALIAS" \
  -storepass "$STORE_PASS" 2>/dev/null | grep "SHA256:"
echo ""

# Encode for GitHub secret
B64=$(base64 -w 0 "$KEYSTORE_FILE")

echo "────────────────────────────────────────────────────────"
echo "Add these to GitHub Repository Secrets:"
echo "(Settings → Secrets and variables → Actions → New repository secret)"
echo ""
echo "SIGNING_KEY_BASE64:"
echo "$B64"
echo ""
echo "SIGNING_KEY_ALIAS:      $KEY_ALIAS"
echo "SIGNING_KEY_PASSWORD:   [the key password you entered]"
echo "SIGNING_STORE_PASSWORD: [the store password you entered]"
echo "────────────────────────────────────────────────────────"
echo ""
echo "⚠️  KEEP $KEYSTORE_FILE SAFE. If you lose it, you cannot update the app."
echo "    Store it in a password manager or encrypted backup."
