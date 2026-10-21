#!/usr/bin/env bash
# Build a cloud-only copy without altering local source, staged save, or Generated save.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/config/tts_workflow.conf"
TTS_SAVES_DIR="${TTS_SAVES_DIR/#\~/$HOME}"
source_save="${1:-$ROOT/output/$TTS_SAVE_NAME}"
cloud_name="Leaf_and_Let_Die_Blind_Playtest_Cloud.json"
cloud_output="$ROOT/output/cloud/$cloud_name"
if [[ ! -f "$source_save" ]]; then
  echo "Missing local source: $source_save" >&2
  echo "Rebuild the staged save first (or pass an explicit local save)." >&2
  exit 2
fi
# First check cards while the source still retains its recognizable local image filenames.
python3 "$ROOT/scripts/validate_card_save.py" "$source_save" --config "$ROOT/config/card_catalog_effects.json"
python3 "$ROOT/scripts/cloud_publish.py" "$source_save" "$cloud_output" \
  --manifest "$ROOT/config/cloud_asset_urls.json" \
  --name 'Leaf & Let Die - Blind Playtest (Cloud)'
# Copy into a different Save & Load folder; never update Generated or an existing Active save.
active_dir="${TTS_SAVES_DIR%/*}/Active"
mkdir -p "$active_dir"
release_target="$active_dir/$cloud_name"
if [[ -f "$release_target" ]]; then
  stamp="$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$ROOT/output/backups"
  cp "$release_target" "$ROOT/output/backups/Cloud_pre_publish_$stamp.json"
fi
cp "$cloud_output" "$release_target"
echo "Release saved into TTS Active: $release_target"
echo 'This script does NOT upload new artwork or update Steam Workshop.'
