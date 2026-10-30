#!/usr/bin/env bash
# Produce a versioned, cloud-asset-backed save without replacing approved Active snapshots.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/config/tts_workflow.conf"
TTS_SAVES_DIR="${TTS_SAVES_DIR/#\~/$HOME}"
source_save="${1:-$ROOT/output/$TTS_SAVE_NAME}"
[[ -f "$source_save" ]] || { echo "Missing local source: $source_save" >&2; exit 2; }
rev="$(python3 - "$ROOT/scripts" "$source_save" <<'PY'
import json, sys
sys.path.insert(0, sys.argv[1])
from save_release import revision
print(revision(json.load(open(sys.argv[2], encoding='utf-8'))))
PY
)"
cloud_name="Leaf_and_Let_Die_Blind_Playtest_${rev}_Cloud.json"
cloud_output="$ROOT/output/cloud/$cloud_name"
python3 "$ROOT/scripts/validate_card_save.py" "$source_save" --config "$ROOT/config/card_catalog_effects.json"
python3 "$ROOT/scripts/cloud_publish.py" "$source_save" "$cloud_output" --manifest "$ROOT/config/cloud_asset_urls.json"
active_dir="${TTS_SAVES_DIR%/*}/Active"
mkdir -p "$active_dir"
release_target="$active_dir/$cloud_name"
if [[ -f "$release_target" ]]; then
 stamp="$(date +%Y%m%d-%H%M%S)"
 mkdir -p "$ROOT/output/backups"
 cp "$release_target" "$ROOT/output/backups/Cloud_${rev}_pre_publish_$stamp.json"
fi
cp "$cloud_output" "$release_target"
echo "Cloud-ready TTS save: $release_target"
echo 'Assets must already be hosted. This does not upload assets, Steam saves or a Workshop item.'
