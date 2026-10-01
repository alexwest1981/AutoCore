#!/usr/bin/env bash
# ==============================================================================
# Wigell AutoCore - Vidarebefordrar till huvudaudit och test-runner test.sh
# ==============================================================================
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$DIR/test.sh" "$@"
