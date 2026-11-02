# Task 7 — Persistent-JVM baseline checkpoint protocol

The opt-in Gradle task `runBaselineResyncCardsChunked` runs all chunks inside a single JVM, while the legacy Bash entry remains unchanged.

Each `sample-000000-000099` chunk has:

- `.tsv.tmp`: uncommitted output, remains after a failed/interrupted run.
- `.tsv`: published output, atomically renamed from the validated temporary TSV.
- `.complete`: Java Properties containing protocol version, full experiment fingerprint, first sample, game count, and SHA-256 of the published TSV. Written to `.complete.tmp` and atomically renamed **last**.
- `.log`: per-chunk START/DONE/SKIP or full exception trace.

On resume, a chunk is skipped **only** if both `.tsv` and `.complete` exist, all marker fields match the current configuration, its SHA-256 matches, and its TSV has the exact header, column count, sequential sample indices, and number of rows expected for all Plant identities. Otherwise the chunk is rerun and its stale marker invalidated. A `.tmp` never qualifies as complete. Interrupted chunks rerun from their first sample, not mid-chunk. Atomic renaming requires the filesystem support `ATOMIC_MOVE`; otherwise the program fails closed. Do not run overlapping writers; the JVM uses a `.lock` file.

The fingerprint includes source identity, absolute sample start, games, chunk size, players, all three seeds, and SHA-256 hashes of both research input files. By default source identity is computed from Kotlin files under `src/main` and `src/simulation` in the working directory. For reproducible archived runs, pass `-Psource-identity="$(git rev-parse HEAD)-$(find src/main src/simulation -name '*.kt' -type f -print0 | LC_ALL=C sort -z | xargs -0 sha256sum | sha256sum | cut -d' ' -f1)"`. A changed revision invalidates old completion markers when a source identity is supplied; the default detects changed Kotlin source bytes but cannot detect changes to compiled dependencies.

Example:

```bash
./gradlew runBaselineResyncCardsChunked -Pgames=1000 -Pchunk=100 -Pplayers=4 \
  -Poutput-dir=build/reports/baseline-resync-chunks
```

Running the identical command again should skip the ten validated chunks. Corrupting a TSV or changing inputs forces its chunk to rerun. The legacy Bash script's manifest, aggregation, per-chunk Gradle logs, and final archive are **not** migrated in Task 7; the new `.log` records Kotlin-level chunk status/errors, not redirected Gradle output. Task 9 integrates the production launcher and Bash publication workflow.

Run targeted regression tests:

```bash
./gradlew simulationTest --tests '*BaselineChunkCheckpointTest' --tests '*BaselineResyncCardBalanceChunkedTest'
```
