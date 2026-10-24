# Task 9 — Direct Java persistent-JVM baseline launcher

## Workflow

`bin/human_baseline_resync_card_balance` still validates input, fingerprints
revision/sources/research CSVs, prepares the manifest, acquires `OUT/.lock`,
preflights the market, aggregates the card report and publishes a validated
archive using a temporary filename.

It now invokes `./gradlew --no-daemon writeSimulationRuntimeClasspath` **once**.
The task depends on `simulationClasses` and writes the full runtime classpath
to `build/simulation-runtime-classpath.txt` (including main/simulation classes,
resources and runtime dependencies).

`bin/lib/run_simulation_java` reads that file and uses `exec java -cp` to start
the Kotlin `BaselineResyncCardBalanceChunkedMainKt` exactly once, passing all
arguments without reinterpretation. `JAVA_OPTS` can provide simple space-delimited
JVM flags, e.g. `JAVA_OPTS='-Xmx4g -XX:+UseG1GC'`.

Kotlin now owns chunk timing, validated `.complete` markers, TSV temporary
publication and per-chunk lifecycle logs. Bash saves `experiment.log` via
`tee`, and `build.log` for Gradle diagnostics. No per-chunk Gradle invocation
remains. `OUT/.lock` (Bash) and `OUT/chunks/.lock` (Kotlin) are distinct.

## Test

1. `bash -n bin/human_baseline_resync_card_balance bin/lib/run_simulation_java`
2. `./gradlew writeSimulationRuntimeClasspath`
3. `GAMES=12 CHUNK=4 bin/human_baseline_resync_card_balance`
4. Repeat the same command: expect three SKIPs from Kotlin and no new games.
5. Remove one completion marker and repeat: expect just one chunk rerun.
6. Verify archive integrity with `tar -tzf <output-archive>.tgz`.
7. Compare aggregate TSV and individual sample TSVs with the Task 1 baseline.

## Boundaries

The original Gradle `runBaselineResyncCards` and
`runBaselineResyncCardsChunked` tasks remain supported for diagnostics.
Only the baseline Bash experiment has been migrated; unrelated experiment
families still require separate review. `bin/benchmark_human_baseline_runtime`
now benchmarks the new launcher; Task 1 measurements remain the legacy
reference for comparison. Successful JVM compilation and full integration
must be established on the user's machine.
