#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/config/tts_workflow.conf"
TTS_SAVES_DIR="${TTS_SAVES_DIR/#\~/$HOME}"
cd "$ROOT"
STAGED="$ROOT/output/$TTS_SAVE_NAME"
ACTIVE="$TTS_SAVES_DIR/$TTS_SAVE_NAME"
if [[ -f "$STAGED" ]]; then SOURCE="$STAGED"
elif [[ -f "$ACTIVE" ]]; then SOURCE="$ACTIVE"
else echo "Missing save: $STAGED or $ACTIVE" >&2; exit 2
fi
# Only replace the staged save after the update fully succeeds.
mkdir -p "$ROOT/output"
TMP="$(mktemp "$ROOT/output/.rulebook-XXXXXX.json")"
trap 'rm -f "$TMP"' EXIT
python3 scripts/update_rulebook.py --save "$SOURCE" --output "$TMP" "${@}"
mv "$TMP" "$STAGED"
echo "Run ./scripts/publish.sh to publish the updated rulebook."
