#!/usr/bin/env bash
# Minimal operational gate for the Leaf simulator. No source/data/weight mutations.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
MODE=quick
case "${1:-}" in
  '') ;;
  --extremes) MODE=extremes; shift ;;
  --help|-h)
    cat <<'HELP'
Usage: bash doc/SIMULATION_OPERATIONAL_READINESS.sh [--extremes]
  default    Unit + integration + simulation tests, full-game smoke, A03 boundaries.
  --extremes Additionally run a SMALL R3B.1 official/disabled/extreme/+20VP
             experimental probe after all operational checks pass. This may
             take substantial time and is NOT full balance certification.
Logs and summary: output/readiness/latest/ (older runs are overwritten).
Exit 0: all requested checks succeeded; nonzero: one or more failed.
HELP
    exit 0 ;;
  *) echo "Unknown argument: $1" >&2; exit 2 ;;
esac
if (($#)); then echo 'Unexpected arguments' >&2; exit 2; fi
OUT="$ROOT/output/readiness/latest"
mkdir -p "$OUT"
REPORT="$OUT/summary.txt"
: > "$REPORT"
failed=0
run_step() {
  local label="$1"; shift
  local log="$OUT/${label}.log"
  printf '\n=== %s ===\n' "$label"
  printf 'RUN %s\n' "$label" >> "$REPORT"
  if "$@" >"$log" 2>&1; then
    printf 'PASS %s\n' "$label" | tee -a "$REPORT"
  else
    local rc=$?
    printf 'FAIL %s (exit %s) — %s\n' "$label" "$rc" "$log" | tee -a "$REPORT"
    tail -n 16 "$log" || true
    failed=1
    return 1
  fi
}
printf 'Leaf operational readiness; mode=%s\n' "$MODE" >> "$REPORT"
printf 'Date: %s\n' "$(date -Is)" >> "$REPORT"
printf 'Commit: %s\n' "$(git rev-parse --short HEAD 2>/dev/null || echo unavailable)" >> "$REPORT"
# Gradle runs in one invocation to avoid repeated initialization / compilation.
run_step regression ./gradlew --no-daemon test integrationTest simulationTest || true
if ((failed == 0)); then
  run_step mechanical-smoke ./gradlew --no-daemon runMechanicalGameSmoke || true
  run_step human-baseline-smoke ./gradlew --no-daemon runHumanBaselineSmoke || true
else
  printf 'SKIP full-game smoke (regressions failed)\n' | tee -a "$REPORT"
fi
run_step protected-boundaries python3 bin/a03_verify_boundaries.py || true
if [[ "$MODE" == extremes ]]; then
  if ((failed == 0)); then
    # This script itself is responsible for its own unique, versioned output.
    # Constrain discovery/holdout sizes but never bypass the real research checks.
    run_step extreme-controls bin/r3b1_effect_isolation \
      --generations 1 --population 3 --elites 1 --games 1 \
      --holdout-games 2 --mutations 1 \
      --output-root output/experiments/readiness-r3b1 || true
  else
    printf 'SKIP extreme controls (operational checks failed)\n' | tee -a "$REPORT"
  fi
else
  printf 'NOT RUN extreme controls (opt in with --extremes)\n' >> "$REPORT"
fi
if ((failed)); then
  printf '\nOPERATIONAL READINESS: FAIL\n' | tee -a "$REPORT"
  printf 'See %s\n' "$REPORT"
  exit 1
fi
if [[ "$MODE" == extremes ]]; then
  printf '\nOPERATIONAL READINESS: PASS (including small experimental probe; NOT balance certification)\n' | tee -a "$REPORT"
else
  printf '\nOPERATIONAL READINESS: PASS (extreme sensitivity NOT tested)\n' | tee -a "$REPORT"
fi
printf 'See %s\n' "$REPORT"
