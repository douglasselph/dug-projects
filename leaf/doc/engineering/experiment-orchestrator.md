# Task 3 — Reusable experiment orchestration foundation

This change introduces package `dugsolutions.leaf.simulation.v35.experiment.orchestration` in the simulation source set. It does **not** migrate existing entry points or modify rules, simulation, AI policies, or output files. That is Task 4/5/6/7 work.

- `ExperimentConfig`: first global sample, total games, and chunk size. This is distinct from the existing seed/Plant-override `experiment.ExperimentConfig`, which remains unchanged.
- `ChunkCoordinator`: stable one-based chunk numbers and global `[start,end)` sample intervals, including a short final chunk.
- `ExperimentTiming`: elapsed monotonic clock, measured-chunk mean, remaining estimate and local ETA. Skipped chunks do not contribute to mean.
- `ExperimentProgress`: typed start/completed/skipped/failed/finished events and an optional console renderer. Console formatting is a starter implementation; it does not yet exactly duplicate the Bash helper's delayed previous-run reporting.
- `ExperimentRunner`: sequential one-JVM chunk/sample callback loop; commits the chunk after sample callbacks succeed. No engine, policy, catalog, Chronicle, or per-game result references are retained by this layer.

## Integration contract

Use a single `ExperimentRunner` per experiment execution. `runSample(sample)` must construct/run/report one game, releasing its references on return. For each chunk, `beginChunk` prepares its temporary output and `commitChunk` atomically seals it and writes its completion marker. The callback `isComplete` reads those markers and decides resume/skip. The runner does **not** implement filesystem writes or enforce cleanup of partial files; those are deliberately left with the experiment-specific adapter for Task 7. State released by `runSample` is reclaimable by GC; the runner does not call `System.gc()`.

Do not share mutable per-game objects through the callbacks. Shared immutable catalogs/weights can live outside the loop. Exception propagation leaves failed chunks uncommitted *provided the caller's commit callback writes completion markers only after a successful atomic commit*.

## Verification

`./gradlew simulationTest --tests '*ExperimentOrchestrationTest'`

Existing research entry points are unchanged, so baseline benchmark comparisons will be relevant after Task 4/5 integration, not yet.
