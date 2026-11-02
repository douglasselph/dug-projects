# Task 10 — persistent-JVM correctness and lifecycle validation

## Tests

- `BaselinePersistentJvmRegressionTest`: compares legacy single-batch TSV byte-for-byte with single-JVM output across chunk sizes 1, 2, 3, 7 and 20, with an absolute starting sample index; checks exact legacy TSV columns, sample coverage, unchanged checkpoint hashes and resimulation of an incomplete chunk.
- `ExperimentFailureRecoveryTest`: injects a failure during a partially written chunk, verifies that it is never marked complete, then resumes without rerunning committed chunks; checks timing's skip exclusion and exception propagation.
- `BaselineLongRunMemoryTest`: opt-in, runs at least 3,000 successive full games in one Koin session, samples post-GC retained heap every 500 games, keeps no per-game references, and checks for gross late-stage heap growth. A passing test is evidence against substantial retention, **not** a proof of freedom from memory leaks. Normal execution never requests GC.

## Commands (from the project root)

```bash
./gradlew simulationTest --tests '*BaselinePersistentJvmRegressionTest' --tests '*ExperimentFailureRecoveryTest' --tests '*ExperimentTimingParityTest' --tests '*BaselineChunkCheckpointTest'
./gradlew simulationTest --tests '*BaselineLongRunMemoryTest' -Dleaf.stress.games=3000
./gradlew test integrationTest simulationTest
```

Long-running checks are explicitly opt-in and do not run games when the system property is absent. For better diagnostic stability, repeat the long test on the same JDK and heap limits. For more detailed analysis, use the existing `-Pmemory-sample-every` / `-Pmemory-retained-every` runner options to record a TSV trend, or profile allocations with JFR. GC and retained-heap measurements occur **only** in the opt-in diagnostic path.

## Deliberate boundaries

No rules, policy behavior, deterministic seed formula, production output schema, or orchestration execution logic is changed. Tests use real game execution for output-equivalence coverage and a synthetic chunk executor only for reproducible fault injection. This set does not simulate OS-level `SIGKILL`, disk-full conditions, or JVM crashes; checkpoint durability across those conditions requires separate end-to-end testing.

## Acceptance

1. Build and run focused tests without failure.
2. Compare TSV rows and hashes from different chunk sizes, including a partial last chunk.
3. Verify missing completion marker re-executes only its chunk; fully validated chunks remain skipped.
4. Confirm thrown execution failure cannot publish the current chunk's marker.
5. Run 3,000+ successive games; inspect full retained heap series, not merely total allocated or committed heap.
