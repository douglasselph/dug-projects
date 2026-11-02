#!/usr/bin/env bash
set -euo pipefail
[[ -f build.gradle.kts && -d src/main/kotlin/dugsolutions/leaf/v35 ]] || {
  echo 'Run from the top-level leaf project directory' >&2; exit 2;
}
obsolete='src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/resource/SunlightPriority.kt'
if [[ -f "$obsolete" ]]; then
  rm -- "$obsolete"
  echo "B03: deleted obsolete scorer $obsolete"
else
  echo 'B03: obsolete scorer is already absent'
fi
