# Leaf & Let Die — TTS tray iteration

## Setup once

Unzip this update into your `leaf-tts` project directory (existing files will be replaced; the VP tray builder is updated). Check `config/tts_workflow.conf`: `TTS_SAVES_DIR` points to the folder where TTS saves are read and written. Working filename: `TS_Save_latest.json`.

Your current working save should first be copied or saved under that name in the configured TTS folder. Keep historical numbered saves until you are comfortable removing them manually.

## Build and publish revised VP tray

```bash
scripts/rebuild_vp.sh
scripts/publish.sh
```

`rebuild_vp.sh` reads the configured current save, creates a revised tray save in `output/TS_Save_latest.json`, and does **not** publish automatically. `publish.sh` backs up the previous live save before copying the staged version. Run after TTS has saved any in-game edits, or they will not be included.

The builder is repeatable: replacing the same model GUID and two copied deck GUIDs instead of duplicating them. It leaves the VP bag in Game Box. It does not modify its input save.

## Exchange

- `scripts/pack.sh`: archives scripts, tools, config, tray artwork, generated tray, full asset-file inventory (`assets/files.txt`), and **the current live working save** in `output/leaf-tts-pack-YYYYMMDD-HHMMSS.tgz`. It also backs up the current save into `output/backups/`. Full `assets/` is NOT included, only `assets/trays/`.
- `scripts/unpack.sh /path/to/assistant-patch.zip`: installs patch files, backs up overwritten local files under `output/unpack-backups/`, and never changes TTS working save directly. A packed `source/TS_Save_latest.json` goes into `output/TS_Save_latest.json` for review.
- `scripts/publish.sh [path/to/save.json]`: explicitly publishes a save to the configured TTS folder, backing up the previous working copy under `output/backups/`.

`output/` remains local scratch/history and is deliberately excluded from packs. `generated/vp_tray/` is included because its OBJ and PNG are **live TTS dependencies**, not disposable scratch. `tools/` remains for now to avoid breaking paths; you can migrate the full-build utilities to `scripts/` later as a distinct change.

## Caveats

The build is not physically play-tested. Local `Z:/home/...` paths are for your Linux/Proton setup. Custom model paths must match your local project root, and TTS may need a reload after updating mesh/texture files. TTS can give a custom model a non-convex collider, but test whether VP decks collide cleanly and can be picked up from the deep tray. The VP artwork has a horizontal printed line; this prototype rotates **only the 3D divider** to north–south as requested.
