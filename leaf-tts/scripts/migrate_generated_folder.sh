#!/usr/bin/env bash
# One-time migration, preserving saves by moving the *whole* folder.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/config/tts_workflow.conf"
new="${TTS_SAVES_DIR/#\~/$HOME}"
old="${new%/*}/Leaf and Let Die - Generated"
if [[ -d "$new" ]]; then
 echo "Destination exists: $new" >&2
 echo 'No changes made. Resolve duplicate folder names manually.' >&2
 exit 2
fi
if [[ ! -d "$old" ]]; then
 echo "Old Generated folder does not exist: $old" >&2
 echo 'No migration required; new folder will be created on publish.'
 exit 0
fi
mv -- "$old" "$new"
echo "Moved old Generated folder to: $new"
