#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/config/tts_workflow.conf"
cd "$ROOT"
ACTIVE="$TTS_SAVES_DIR/$TTS_SAVE_NAME"
STAGED="$ROOT/output/$TTS_SAVE_NAME"
if [[ -f "$STAGED" ]]; then
  SAVE="$STAGED"
elif [[ -f "$ACTIVE" ]]; then
  SAVE="$ACTIVE"
else
  echo "Missing active or staged save: $ACTIVE" >&2; exit 2
fi
python3 scripts/build_wisp_butterfly_tray.py --save "$SAVE"
mkdir -p output
cp generated/wisp_butterfly_tray/Wisp_Butterfly_Tray_Test_Save.json "output/$TTS_SAVE_NAME"
echo "Staged: output/$TTS_SAVE_NAME"
echo "Review, then run scripts/publish.sh (close/reload TTS as needed)."
