#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/config/tts_workflow.conf"
TTS_SAVES_DIR="${TTS_SAVES_DIR/#\~/$HOME}"
cd "$ROOT"
STAGED="$ROOT/output/$TTS_SAVE_NAME"
ACTIVE="$TTS_SAVES_DIR/$TTS_SAVE_NAME"
if [[ -f "$STAGED" ]]; then SAVE="$STAGED"
elif [[ -f "$ACTIVE" ]]; then SAVE="$ACTIVE"
else echo "Missing save: $STAGED or $ACTIVE" >&2; exit 2
fi
python3 scripts/build_game_box.py --save "$SAVE"
cp generated/game_box/Game_Box_Test_Save.json "$STAGED"
echo "Game Box board and contents staged in $STAGED. Run ./scripts/publish.sh."
