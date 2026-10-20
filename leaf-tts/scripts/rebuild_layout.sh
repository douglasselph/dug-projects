#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/config/tts_workflow.conf"
TTS_SAVES_DIR="${TTS_SAVES_DIR/#\~/$HOME}"
cd "$ROOT"
SAVE="$ROOT/output/$TTS_SAVE_NAME"
if [[ ! -f "$SAVE" ]]; then
  if [[ -f "$TTS_SAVES_DIR/$TTS_SAVE_NAME" ]]; then
    mkdir -p output
    cp "$TTS_SAVES_DIR/$TTS_SAVE_NAME" "$SAVE"
  else
    echo "No staged or published TTS save found" >&2; exit 2
  fi
fi
python3 scripts/generate_card_config.py
python3 scripts/apply_game_box_layout.py --save "$SAVE"
echo 'Layout staged. Run ./scripts/publish.sh.'
