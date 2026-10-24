# Task 6 — Kotlin timing parity

The single-JVM `ExperimentRunner` now reproduces the progress cycle of `bin/lib/experiment_run_timing.sh`:

- A one-based `Run X of Y starting: ...` banner appears for every chunk, including skipped chunks.
- On entering the next chunk, a completed **non-skipped** previous chunk produces `Previous run took ...; average .../run; estimated remaining ...; ETA ...` before the next heading.
- A skipped chunk is not timed or included in the average. Its heading carries `SKIP`; the next start does not report the skipped slot's duration.
- After the final non-skipped chunk, the footer includes `Final run took ...; average .../run; measured phase total ...`; if the final slot was skipped, no final-run timing appears (matching the shell helper).
- The measured phase total excludes skipped chunks. The footer also reports completed/skipped counts.
- Elapsed time uses `System.nanoTime()`; ETA uses an injected wall-clock `Clock` and formats in the runtime's local timezone, consistent with `date -d` in Bash.
- The estimate counts all remaining **slots**, even those that might later be skipped, matching the shell helper (and is therefore conservative on resumed runs).
- For each chunk, timing covers begin, sample evaluation, and successful commit. This is slightly narrower than the shell helper's wall-clock measurement between start markers: chunk skip checks and console progress display are excluded. Reporting rounds down to whole seconds just as Bash does.
- Existing `ExperimentProgress.Completed` events remain emitted for API consumers, but the console no longer prints them. It instead prints `PreviousRunTiming` before the next run banner, and reports final-run timing in `Finished`.

No gameplay rules, RNG behavior, chunk destinations, or Bash experiment scripts are changed. Chunk atomicity, `.complete` markers, and resume verification are scheduled for Task 7.

## Verification

```bash
./gradlew simulationTest --tests '*ExperimentOrchestrationTest' --tests '*ExperimentTimingParityTest'
```

The individual orchestration sources were compiled with `kotlinc` in the packaging environment. Gradle tests could not be run there because the Gradle 8.13 distribution was not cached and network resolution failed.
