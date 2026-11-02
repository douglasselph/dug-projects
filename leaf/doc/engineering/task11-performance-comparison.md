# Task 11 — baseline performance and correctness comparison

Run from the root of the Git checkout:

```bash
bin/benchmark_persistent_jvm_comparison.py --games 1000 --chunk 100
```

For a fast preflight:

```bash
bin/benchmark_persistent_jvm_comparison.py --games 12 --chunk 4
```

For a larger confidence run:

```bash
JAVA_OPTS='-Xmx4g' bin/benchmark_persistent_jvm_comparison.py --games 3000 --chunk 100
```

The benchmark builds and exports a classpath **once**, then runs the same currently compiled simulator bytecode two ways:

1. Legacy Main: a fresh Java process for each `--chunk` samples.
2. Persistent Main: a single Java process for all samples, writing multiple chunks.

The legacy experiment was originally invoked through Gradle per chunk. We intentionally do not invoke Gradle in the timed legacy loop: this isolates repeat JVM/catalog/Koin initialization and JIT warm-up costs. Gradle build time is timed separately. The Task 1 historical measurements remain the valid estimate of the *old combined Gradle + JVM-per-chunk* behavior.

Reports are created under `output/benchmarks/persistent-jvm-comparison/<timestamp>-pid*/`:

- `comparison.json`: test configuration, Git SHA, input SHA-256, elapsed times, games/sec, speedup, Java launches, RSS, normalized output hashes, per-chunk hashes, memory checkpoints, and correctness status.
- `comparison.md`: concise summary.
- `legacy/sample-*.tsv`, `persistent/sample-*.tsv`: primary results.
- `legacy/sample-*.log`, `legacy/sample-*.time`, `persistent.log`, `persistent.time`: raw evidence.
- `persistent/memory-diagnostics.tsv`: opt-in ordinary heap and post-GC retained heap checkpoints. The default interval is 100/500 games.

**Acceptance condition:** exact TSV header and all result rows must match after ordering by global sample index. This comparison deliberately ignores differences in file path and order of chunks, but does *not* round floating-point fields, ignore columns, or tolerate changed values. Both modes use the same sample indices and same RNG seeds. A mismatch exits nonzero and retains artifacts.

**Limitations and interpretation:**

- `Maximum resident set size` is OS process peak RSS, not live Java heap. Legacy RSS reports maximum of its separate JVMs; summing per-process peaks is not meaningful for sequential execution.
- A minimal five-run `java -version` launch probe is a *lower-bound approximation* of JVM launch cost; runtime differences also include simulator setup and JIT effects. Do not attribute all savings to JVM startup alone.
- Diagnostic GC can perturb performance, so for a **fair speed benchmark**, pass `--memory-sample-every 0 --memory-retained-every 0`. Use a separate run with memory diagnostics enabled to study post-GC retained heap.
- Check the retained-heap checkpoints for convergence rather than expecting committed JVM heap or RSS to shrink after each game.
- Exact baseline row equivalence means no observed rules/policies/scoring/output changes for tested samples; it does not formally prove the absence of changes for all possible states. Run `./gradlew test integrationTest simulationTest` separately.
- Repeat several runs under similar machine load and consider reversing run order to guard against thermal/caching bias. The provided harness runs legacy first and persistent second.
- The harness creates unique output directories, avoiding accidental resume-skips that would invalidate timings.
