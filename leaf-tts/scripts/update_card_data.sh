#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
python3 scripts/generate_card_config.py
# Refresh names, effects, and permanent card IDs on already-staged saves.
./scripts/rebuild_layout.sh
