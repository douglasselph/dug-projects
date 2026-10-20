#!/usr/bin/env bash
# Run the unpacker directly so it can safely update tts_workflow.sh itself.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
archive="${1:?Usage: scripts/unpack.sh path/to/patch.zip-or-tgz}"
[[ -f "$archive" ]] || { echo "No such archive: $archive" >&2; exit 2; }
BACKUP_DIR="$ROOT/output/unpack-backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
python3 "$ROOT/scripts/safe_unpack.py" "$archive" "$ROOT" "$BACKUP_DIR"
echo "Files installed. Replaced-file backups (if any): $BACKUP_DIR"
echo "Live TTS save was NOT changed. Run scripts/publish.sh after reviewing."
