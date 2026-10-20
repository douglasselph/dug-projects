#!/usr/bin/env bash
set -euo pipefail
exec "$(dirname "$0")/tts_workflow.sh" publish "$@"
