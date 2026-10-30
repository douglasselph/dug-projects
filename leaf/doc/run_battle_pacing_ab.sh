#!/usr/bin/env bash
# Noncanonical research runner; Gradle build configuration is unchanged.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
./gradlew writeSimulationRuntimeClasspath
CLASSPATH="$(cat build/simulation-runtime-classpath.txt)"
exec java -Xmx3g -cp "$CLASSPATH" \
  dugsolutions.leaf.simulation.v35.experiment.battlepacing.BattlePacingABMainKt "$@"
