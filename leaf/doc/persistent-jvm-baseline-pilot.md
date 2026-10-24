# Task 5: single-JVM baseline resync chunk pilot

This is an opt-in Kotlin entry point; **the existing `bin/human_baseline_resync_card_balance` remains unchanged** and still runs Gradle once per chunk. Task 9 migrates shell launch infrastructure; Task 7 adds resume markers and atomic output publishing.

## Invocation

From project root:

```bash
./gradlew runBaselineResyncCardsChunked \
  -Pgames=1000 -Pchunk=100 -Pplayers=4 \
  -Poutput-dir=build/reports/baseline-resync-chunks
```

Optional properties: `start`, `seed`, `strategy-seed`, `grove-seed`, `plant-overrides`, `round-overrides`. Defaults are exactly the legacy baseline options. To avoid overwriting unrelated results, use a fresh output directory for each pilot run.

The run creates `sample-000000-000099.tsv` etc. with the exact legacy TSV header and per-card rows in sample order; a short final chunk is supported. The Kotlin ExperimentRunner wraps all chunks within the **same process and same Koin/catalog session**. Each game still receives its own Game, Chronicle, RNG state and policy factories. The global sample number, `seed + sample`, `strategySeed + sample`, and Grove lookup sample are unchanged.

Note: the new opt-in Main presently writes TSV files and progress only. The Bash manifest, flock locking, `.complete` markers, per-chunk log redirection, aggregate report, and result archive are **not** yet migrated. Do not replace the production Bash runner until Tasks 6, 7, and 9. Per-chunk data is published by moving `.tmp` to `.tsv`; atomic rename and resume validation belong to Task 7.

## Verification

`./gradlew simulationTest --tests '*BaselineResyncCardBalanceChunkedTest'` compares a three-game direct legacy batch against two persistent-JVM chunks for exact TSV line equality, and verifies absolute chunk filenames with nonzero start and a partial final chunk. For larger-scale performance use the Task 1 benchmark profiles after the shell launcher is migrated.
