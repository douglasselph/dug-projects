#!/usr/bin/env bash
# Noncanonical A/B research: compile once, run the entire request in one Java JVM.
# Archive the resulting data in ~/Downloads, including incomplete data on failure.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# Match the Kotlin CLI's output default; do not alter any experiment arguments.
OUT="output/experiments/battle-pacing/checkpoint3"
ARGS=("$@")
for ((i=0; i<${#ARGS[@]}; i++)); do
  if [[ "${ARGS[i]}" == "--out" ]] && (( i+1 < ${#ARGS[@]} )); then
    OUT="${ARGS[i+1]}"
  fi
done

./gradlew writeSimulationRuntimeClasspath

# Unlike exec, retain the shell to publish results even when the JVM aborts.
set +e
JAVA_OPTS="${JAVA_OPTS:--Xmx3g}" \
  bin/lib/run_simulation_java \
    dugsolutions.leaf.simulation.v35.experiment.battlepacing.BattlePacingABMainKt "$@"
status=$?
set -e

if [[ -d "$OUT" ]]; then
  out_abs="$(realpath "$OUT")"
  out_parent="$(dirname "$out_abs")"
  out_name="$(basename "$out_abs")"
  mkdir -p "$HOME/Downloads"
  if (( status == 0 )); then tag="complete"; else tag="failed"; fi
  dest="$HOME/Downloads/battle-pacing-${out_name}-${tag}-$(date -u +%Y%m%dT%H%M%SZ)-$$.tgz"
  tmp="$(mktemp "$HOME/Downloads/.battle-pacing.XXXXXXXX.tgz")"
  trap 'rm -f -- "$tmp"' EXIT
  tar -czf "$tmp" -C "$out_parent" "$out_name"
  mv -- "$tmp" "$dest"
  trap - EXIT
  printf 'RESULTS_ARCHIVE %s\n' "$dest"
fi
exit "$status"
