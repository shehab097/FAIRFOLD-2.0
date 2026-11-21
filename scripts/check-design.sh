#!/usr/bin/env bash
# scripts/check-design.sh  (run before every commit and in CI)
set -u
cd "$(dirname "$0")/../frontend" || exit 1
if [ ! -d src ]; then
  echo "design check skipped (frontend/src not found)"
  exit 0
fi
cd src || exit 1

bad=$(grep -rnE "bg-gradient-" . \
        --include=*.ts --include=*.tsx --include=*.css || true)

hex=$(grep -rnE "#[0-9A-Fa-f]{6}\b" . --include=*.ts --include=*.tsx --include=*.css \
      | grep -vE "styles/tokens.css|charts/defaults.ts" || true)

if [ -n "$bad" ] || [ -n "$hex" ]; then
  [ -n "$bad" ] && { echo "Forbidden gradient:"; echo "$bad"; }
  [ -n "$hex" ] && { echo "Colours must come from tokens:"; echo "$hex"; }
  exit 1
fi
echo "design check passed"
