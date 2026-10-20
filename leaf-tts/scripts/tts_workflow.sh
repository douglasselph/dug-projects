#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/config/tts_workflow.conf"
TTS_SAVES_DIR="${TTS_SAVES_DIR/#\~/$HOME}"
BACKUP_DIR="$ROOT/output/backups"
mkdir -p "$BACKUP_DIR" "$ROOT/output"
case "${1:-}" in
 pack)
   now="$(date +%Y%m%d-%H%M%S)"
   tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
   mkdir -p "$tmp/leaf-tts/source" "$tmp/leaf-tts/assets"
   cd "$ROOT"
   # Share asset filenames without copying the whole asset library.
   if [[ -d assets ]]; then find assets -type f | LC_ALL=C sort > "$tmp/leaf-tts/assets/files.txt"; fi
   # Include the small tray source-art subset so revisions remain reproducible.
   [[ ! -d assets/trays ]] || cp -a assets/trays "$tmp/leaf-tts/assets/"
   for dir in scripts tools config generated/vp_tray generated/wisp_butterfly_tray generated/token_tray generated/dice_trays generated/player_box generated/game_box; do
     if [[ -d "$dir" ]]; then mkdir -p "$tmp/leaf-tts/$(dirname "$dir")"; cp -a "$dir" "$tmp/leaf-tts/$dir"; fi
   done
   # The live working TTS save has precedence, then local output fallback.
   save=""
   for candidate in "$TTS_SAVES_DIR/$TTS_SAVE_NAME" "$ROOT/output/$TTS_SAVE_NAME"; do
     if [[ -f "$candidate" ]]; then save="$candidate"; break; fi
   done
   if [[ -z "$save" ]]; then
      echo "ERROR: No $TTS_SAVE_NAME in TTS saves or output. Export/name the active save first." >&2; exit 2
   fi
   cp "$save" "$tmp/leaf-tts/source/$TTS_SAVE_NAME"
   cp "$save" "$BACKUP_DIR/TS_Save_${now}.json"
   archive="$ROOT/output/leaf-tts-pack-${now}.tgz"
   tar -czf "$archive" -C "$tmp" leaf-tts
   mkdir -p "$HOME/Downloads"
   cp "$archive" "$HOME/Downloads/$(basename "$archive")"
   echo "Pack created: $archive"
   echo "Pack copied to: $HOME/Downloads/$(basename "$archive")"
   echo "Backup created: $BACKUP_DIR/TS_Save_${now}.json"
   ;;
 unpack)
   archive="${2:?Usage: scripts/unpack.sh path/to/patch.zip-or-tgz}"
   [[ -f "$archive" ]] || { echo "No such archive: $archive" >&2; exit 2; }
   stamp="$(date +%Y%m%d-%H%M%S)"
   mkdir -p "$BACKUP_DIR" "$ROOT/output/unpack-backups"
   backup="$ROOT/output/unpack-backups/$stamp"
   mkdir -p "$backup"
   # Never blindly overlay an unknown/untrusted archive. Validate all paths.
   python3 "$ROOT/scripts/safe_unpack.py" "$archive" "$ROOT" "$backup"
   echo "Files installed. Replaced-file backups (if any): $backup"
   echo "Live TTS save was NOT changed. Run scripts/publish.sh after reviewing."
   ;;
 publish)
   candidate="${2:-$ROOT/output/$TTS_SAVE_NAME}"
   [[ -f "$candidate" ]] || { echo "Missing save: $candidate" >&2; exit 2; }
   mkdir -p "$TTS_SAVES_DIR"
   target="$TTS_SAVES_DIR/$TTS_SAVE_NAME"
   now="$(date +%Y%m%d-%H%M%S)"
   if [[ -f "$target" ]]; then cp "$target" "$BACKUP_DIR/TS_Save_pre_publish_${now}.json"; fi
   cp "$candidate" "$target"
   echo "Published: $target"
   echo "Old working save (if any) backed up in: $BACKUP_DIR"
   echo "Open TTS and load '$TTS_SAVE_NAME' from your Generated saves folder."
   ;;
 *) echo "Usage: $0 {pack|unpack ARCHIVE|publish [SAVE.json]}" >&2; exit 2;;
esac
