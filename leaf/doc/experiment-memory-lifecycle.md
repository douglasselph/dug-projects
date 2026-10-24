# Task 8: Persistent-JVM memory lifecycle

The baseline experiment uses a shared Koin/catalog/session scope, but creates Game,
Chronicle, RNG and players per `BaselineResyncCardBalanceSession.runSample()`.
`BaselineResyncCardBalanceReporter.writeGame()` emits TSV rows directly without
keeping Game or result references. `ExperimentRunner` passes sample numbers rather
than games between runs. Chunk output uses a single PrintWriter, closed on chunk
commit/failure. The outer session and Koin application are closed at run exit.

Chronicle and intermediate reporting lists are bounded by a single game's lifecycle;
there is no experiment-wide collection of game chronicles. The research catalogs
and resolved policy inputs are intentionally shared and must not be mutated during
an experiment. The `ExperimentMemory` callback runs only AFTER runSample returns,
so the completed Game is no longer referenced by that call stack.

## Diagnostic run (opt-in; NOT for benchmark timing)

Add the following to the chunked Main args:

```
--memory-sample-every=100 --memory-retained-every=500
```

This writes `memory-diagnostics.tsv` under the selected output directory,
streaming one row every 100 NEW simulated games. Every 500 new games, the
monitor requests two diagnostic garbage collections and records post-GC heap
usage in `retained_heap_bytes`. The ordinary `heap_used_bytes` is measured before
those collections. Blank retained values mean no diagnostic GC was requested.

**Do not combine these diagnostics with performance benchmarking:** diagnostic
GC pauses perturb runtime. Do not infer a leak from increasing committed heap;
instead compare post-GC retained usage after warmup at repeated checkpoints.
This is a heuristic, not an exact leak detector. JVM allocations/GC are normally
managed automatically; there is NO forced GC in the default simulation path.

A previous diagnostic file is overwritten on a new invocation, including resume;
its counter counts only games newly executed in that invocation. TSV checkpoint
logic remains separate. Use a fresh output directory for long diagnostic tests.

## Validation

Run `./gradlew simulationTest --tests '*ExperimentMemoryTest'` followed by the
existing baseline/chunk parity tests. For a stress run, simulate several thousand
games, inspect `retained_heap_bytes` trends, and use JFR or a heap dump to diagnose
any sustained increase. Avoid asserting fixed heap values in CI (GC is non-deterministic).
