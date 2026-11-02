# Task 1 — Baseline before persistent-JVM refactor

## Scope

This task adds **measurement only**. The existing `bin/human_baseline_resync_card_balance`,
`bin/lib/experiment_run_timing.sh`, Gradle `runBaselineResyncCards` task, game engine,
random streams, and reporting formats remain unchanged.

## Current execution verified from the supplied archive

- Baseline script defaults: `PLAYERS=4`, `GAMES=2400`, `CHUNK=100`.
- For each incomplete chunk the script invokes `./gradlew --no-daemon runBaselineResyncCards`
  and Gradle's JavaExec task launches the Kotlin main. This is **one new simulation
  JVM per chunk**, not one JVM per game.
- Within a chunk, `BaselineResyncCardBalanceMain.kt` calls `repeat(games)`,
  constructs a new `Game` using `GameFactory`, and calls `GameRunner.run(game)`.
- Sample-indexed randomness uses `seed + sample` and `strategySeed + sample`;
  the Grove selection is also indexed by sample with the dedicated Grove seed.
- Chunk output is written to `sample-START-END.tsv.tmp`, moved to the final TSV on
  success, then a `.complete` marker is created. Completed chunks can be skipped.
- `experiment_run_timing.sh` reports run X/Y, previous/average duration, ETA,
  and a final measured duration; SKIP slots are excluded from duration averages.
- After all chunks finish, `bin/aggregate_human_resync_cards.py` runs and the output
  directory is archived.

## Fixed baseline invocations

Run from a **Git checkout** of the current source (not just the exported `.tgz`):

```bash
bin/benchmark_human_baseline_runtime verify
bin/benchmark_human_baseline_runtime benchmark
```

Fixed configurations (only benchmark destination is configurable):

| Profile | Players | Games | Chunk | Seed | Strategy seed | Grove seed |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| verify | 4 | 12 | 4 | 120000 | 220000 | 320000 |
| benchmark | 4 | 1000 | 100 | 120000 | 220000 | 320000 |

Set `BENCHMARK_ROOT` if you want results somewhere other than
`output/benchmarks/baseline-runtime`. **Do not compare against a resumed
or cache-warmed run without recording that condition**; each invocation uses a
new results directory and is intended to be run fully from scratch.

## Artifacts

Each profile creates a distinct UTC-timestamped benchmark directory containing:

- `config.txt`, `git-head.txt`, `input-sha256.txt`, `java-version.txt`, `gradle-version.txt`
- `time.txt` (`/usr/bin/time -v`), `stdout.log`, `stderr.log`, `exit-status.txt`
- `java-vmhwm-samples.tsv` (second-resolution Java-descendant **process RSS** high-water sampling)
- `chunk-sha256.txt` (checksums of uncompressed canonical chunk TSVs)
- `completed-chunks.txt`, `summary.json`, and the entire existing `experiment/` results tree

Capture from `time.txt` **elapsed wall-clock time** and **maximum resident set size**.
Important: the time utility's maximum resident set size is **not** a reliable
aggregate of JVM memory across all child JVMs. The sampled Java `VmHWM`
values are peak **process RSS**, not peak **Java heap**. For an accurate peak
**live/committed Java heap** comparison, add JVM-level telemetry (JFR/JMX)
in a later focused measurement step; do not label RSS as Java heap.

`summary.json` checks expected chunk and sample counts and records the TSV
column names. It does **not** prove identical game logic. To compare an old
run and a new persistent-JVM implementation, require equal inputs, seeds,
source/game logic, sampling, and final normalized TSV data; use byte-for-byte
chunk TSV SHA-256 checksums where output ordering is preserved.

## Acceptance criteria for this baseline task

1. `verify` exits 0, produces 3 complete chunks and samples 0 through 11.
2. `benchmark` exits 0, produces 10 complete chunks and samples 0 through 999.
3. Save both summary files, checksums, logs, and timings for comparison.
4. Confirm no baseline simulator or existing timing code was modified.
5. Measure process RSS, explicitly mark JVM heap as **not yet measured**.
6. Document Git SHA, tool versions and research-input SHA-256 hashes.

## Current archive limitation

The supplied source `.tgz` contains no `.git` directory, so the unchanged
baseline script fails its `git rev-parse HEAD` step here. The archive is not a
runtime result and cannot establish measured throughput or memory. Run the
commands in the original Git checkout with its normal Gradle dependency caches.
