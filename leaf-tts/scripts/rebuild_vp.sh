#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/config/tts_workflow.conf"
cd "$ROOT"
SAVE="$TTS_SAVES_DIR/$TTS_SAVE_NAME"
[[ -f "$SAVE" ]] || { echo "Missing active save: $SAVE" >&2; exit 2; }
python3 scripts/build_vp_tray.py --save "$SAVE"
mkdir -p output
cp generated/vp_tray/VP_Tray_Test_Save.json "output/$TTS_SAVE_NAME"
echo "Staged: output/$TTS_SAVE_NAME"
echo "Review, then run scripts/publish.sh (close/reload TTS as needed)."
