# Leaf & Let Die — simulation launch architecture and cleanup guide

**As inspected:** source archive `leaf(20261024-132719).tgz` (October 24, 2026).  This is the handoff document for a new coding conversation. Read the **current source** before editing; this guide does not supersede live implementation.

## 1. Critical distinction: process scope

- **Baseline resync card-balance batch:** `bin/human_baseline_resync_card_balance` runs one **persistent JVM** for the whole experiment. The Kotlin chunked runner performs many games and writes individual resumable chunk files without restarting the JVM.
- **Policy evaluation/training wrappers:** each invocation starts **one JVM for its whole evaluation/training request**, not one JVM per game. A separate training run or evaluation condition remains a separate process to isolate mutable learner/policy state.
- **Complex and historical experiment scripts:** many still use `./gradlew` for specialized phases and/or keep distinct processes. **Do not** automatically convert these to one JVM without reviewing isolation and result contracts.
- Do not confuse the **Gradle build process**, **one Java process**, **one experiment condition**, **one TSV chunk**, and **one game**.

## 2. Compile once and execute directly

From the project root (`leaf`):

```bash
./gradlew --no-daemon writeSimulationRuntimeClasspath
```

This Gradle task depends on `simulationClasses`; it generates `build/simulation-runtime-classpath.txt` for main/simulation classes, resources and dependencies. **Rebuild after changing Kotlin sources or dependencies**; the classpath manifest's existence is NOT a freshness check.

The lowest-level launcher is:

```bash
bin/lib/run_simulation_java <fully.qualified.KotlinMainKt> [arguments...]
```

It reads the classpath file and `exec`s `java -cp ...`; it does **not** compile. Arguments are passed as discrete tokens. Optional simple JVM flags are provided with `JAVA_OPTS='-Xmx4g'` (whitespace-split; do not rely on complex shell quoting in `JAVA_OPTS`).

Use the task dispatcher when a supported named Gradle simulation task has been migrated:

```bash
bin/lib/run_simulation_task runEvaluateBattleMainPolicy --help
LEAF_SIM_LAUNCH=java bin/evaluate_battle_main_policy --help
LEAF_SIM_LAUNCH=gradle bin/evaluate_battle_main_policy --help
```

`run_simulation_task` supports 17 entry points: eight `runTrain*` policy families and nine `runEvaluate*` families (the ninth evaluator is policy interactions). It passes command-line arguments directly to the corresponding Kotlin Main; the Gradle fallback uses the historic `-Pargs` mechanism. `LEAF_SIM_LAUNCH` defaults to `java`; `LEAF_SIM_AUTO_BUILD=1` only builds if the classpath file is **missing**, not when it is stale. For reproducible research, run the explicit build command above first. See `bin/lib/run_simulation_task` for the authoritative task-to-Main mapping.

Current top-level wrappers for these families include `bin/train_{buy,cultivation_main,cultivation_support,battle_main,battle_support,wisp,plant_effect,mulch_target}_policy` and corresponding `bin/evaluate_..._policy`, plus `bin/evaluate_policy_interactions`. The wrappers remain the user-facing interface; **do not remove them**.

## 3. Baseline: a full multi-chunk experiment in one JVM

```bash
GAMES=1000 CHUNK=100 PLAYERS=4 bin/human_baseline_resync_card_balance
```

The Bash wrapper:

1. Validates players/games/chunk and Plant/Round CSV input files.
2. Collects revision, source hash, research-input hashes, seeds and a deterministic run ID.
3. Creates a manifest, validates the market, and holds an outer `flock` lock.
4. Calls `writeSimulationRuntimeClasspath` **once**.
5. Starts one JVM via `bin/lib/run_simulation_java` and `BaselineResyncCardBalanceChunkedMainKt`.
6. Validates expected `.complete` markers, aggregates output, then publishes a checked `.tgz` archive atomically.

The Kotlin baseline session initializes Koin, catalogs, research overrides, factory and runner once. The chunked orchestrator executes separate **games** with stable global sample indices and independent per-game state. The default seeds in this Bash wrapper are mechanical `120000`, strategy `220000`, and Grove `320000`; the per-game mechanical/strategy seed derivations use the global sample offset, not the current chunk offset. Do not alter seeds, processing order, policy, scoring or TSV schema during launch-only changes.

Each completed chunk gets `sample-XXXXXX-XXXXXX.tsv`, a `.complete` marker and a per-chunk `.log`, with a `.tsv.tmp` during publication. Checkpoints are validated against sample counts/ranges, header/schema, hashes and run inputs; invalid/incomplete chunks are rerun. Timing/progress moved to Kotlin for this baseline: `Run X of Y`, prior duration, average of **non-skipped** chunks, remaining-time prediction, local ETA and final totals. Normal runs do not force GC. Optional `MEMORY_SAMPLE_EVERY` and `MEMORY_RETAINED_EVERY` are passed to Kotlin and permit retained-heap diagnostics; the latter uses explicit diagnostic GC only at checkpoints.

See `doc/experiment-checkpointing.md`, `doc/experiment-memory-lifecycle.md`, `doc/experiment-timing-kotlin.md`, and `doc/task9-direct-java-launcher.md` for implementation details.

## 4. Evaluation, training, and multi-condition isolation

Use the named top-level wrappers for policy runs; do **not** call arbitrary Kotlin Mains by guessing class names.

```bash
./gradlew writeSimulationRuntimeClasspath
LEAF_SIM_LAUNCH=java bin/evaluate_battle_main_policy <documented-evaluator-args>
LEAF_SIM_LAUNCH=java bin/train_battle_main_policy <documented-trainer-args>
```

`bin/experiment_policy_interactions` can compile once before its condition loop, but launches each condition in its **own JVM**. Keep that process boundary: independent condition state, failures, output and frozen-policy provenance are essential. It still sources `bin/lib/experiment_run_timing.sh`; do not delete that helper. Other active experiments also source it, including `experiment_balanced_market_sweep`, `experiment_card_tier_calibration`, `experiment_joint_buy_round_learning`, `human_expert_resync_batch`, and `experiment_battle_plant_activation_diagnostic`.

A safe migration of another experiment requires auditing: CLI options, task-to-Main mapping, Gradle `-P` translations, current environment and seed settings, data/weight loading, mutable policy and singleton lifetime, output/marker/lock contracts, failure recovery, input fingerprints, and parity against a real legacy run. Keep separate JVMs when one run can affect the next.

## 5. Verification and performance comparison

```bash
./gradlew test integrationTest simulationTest
bin/benchmark_persistent_jvm_comparison.py --games 12 --chunk 4
bin/benchmark_persistent_jvm_comparison.py --games 1000 --chunk 100 \
  --memory-sample-every 0 --memory-retained-every 0
```

Task 11's comparison harness invokes the original **single-batch Main** in separate JVMs per chunk versus the persistent **chunked Main** in one JVM. It does not compare repeated Gradle configurations; build cost is separate. Examine normalized TSV content, per-sample seeds, game statistics, output SHA-256, process RSS, wall-clock and games/sec. **RSS is not post-GC retained heap.** See `doc/task10-persistent-jvm-validation.md` and `doc/task11-performance-comparison.md`.

The harness and regression tests existing in the archive are not proof they passed in a user's checkout. A new conversation must request/inspect actual result reports before claiming correctness, speedup or bounded live memory.

## 6. Cleanup policy and exact files

The current archive already places **26 historical one-off scripts** under `bin/legacy/` and **17 pre-migration wrapper snapshots** under `bin/legacy/launchers/`. None of those 26 historical *basenames* is referenced by scripts still directly in `bin/` or `bin/lib/` in the inspected archive. Some historical `doc/` files still describe their old `bin/<name>` locations, so removing them permanently may sacrifice historical reproducibility. Do not confuse "unused by active Bash scripts" with "of no research value".

**Recommended immediate cleanup:** leave the live `bin/train_*`, `bin/evaluate_*`, both `bin/lib/run_simulation_*`, the older *single-batch Kotlin Main* for regression, and `bin/lib/experiment_run_timing.sh` in place. Keep `bin/legacy/launchers/` until matched direct-Java/Gradle outputs pass parity. Then the 17 snapshot files can be deleted (exact command below), because the live top-level wrappers already retain `LEAF_SIM_LAUNCH=gradle` as a fallback.

**Historical cleanup, optional:** the 26 `bin/legacy/` root-level files can be deleted from the working tree **only after** confirming they are committed to Git or otherwise archived and that their experiments no longer need to be rerun. They are old one-off studies, not necessarily failed programs. Do not delete their historical result archives, associated data or experiment docs automatically.

**Do not delete** `bin/benchmark_human_baseline_runtime` blindly: it now invokes the migrated baseline wrapper and no longer measures the original Gradle-per-chunk implementation despite its original comment. Retain or explicitly retire it as a redundant benchmarking helper after adopting `bin/benchmark_persistent_jvm_comparison.py`. Do not delete the latter.

### Commands, from the project root

Review first:

```bash
find bin/legacy -maxdepth 1 -type f -print | sort
find bin/legacy/launchers -maxdepth 1 -type f -print | sort
# Verify tracked files and keep evidence in Git history:
git status --short
git ls-files bin/legacy
```

Remove **only the 17 redundant launcher snapshots**, *after* parity is established:

```bash
# If tracked, records proper Git deletions; if untracked, use rm instead.
git rm -- bin/legacy/launchers/{evaluate_battle_main_policy,evaluate_battle_support_policy,evaluate_buy_policy,evaluate_cultivation_main_policy,evaluate_cultivation_support_policy,evaluate_mulch_target_policy,evaluate_plant_effect_policy,evaluate_policy_interactions,evaluate_wisp_policy,train_battle_main_policy,train_battle_support_policy,train_buy_policy,train_cultivation_main_policy,train_cultivation_support_policy,train_mulch_target_policy,train_plant_effect_policy,train_wisp_policy}
```

The command explicitly names the **17** snapshot files observed in this archive. It leaves current `bin/evaluate_*` and `bin/train_*` commands untouched.

Optional historical one-off cleanup, only after review:

```bash
# Review exactly what would be removed:
find bin/legacy -maxdepth 1 -type f -print | sort
# Remove only root-level historical scripts, never bin/legacy/launchers:
find bin/legacy -maxdepth 1 -type f -exec git rm -- {} +
```

If any file is untracked, Git cannot remove it; use `rm -- <verified-path>` explicitly. Do not execute the optional historical deletion as part of an automated install or patch extraction. After any deletion, rerun Bash syntax and a small simulation, and inspect `git diff --cached --stat` before committing.

## 7. Instructions to a new coding conversation

1. Read this document, `doc/USING_THE_SIMULATOR.md`, `doc/task12-experiment-family-migration.md` and the **current** `bin/`/`build.gradle.kts` code.
2. Identify whether the target is one persistent baseline batch, one training/evaluation process, or a deliberately isolated multi-condition study.
3. Compile once explicitly before direct Java; never confuse classpath existence with source freshness.
4. Keep CLI, deterministic seeds, output format, hashes, manifest, logging, timing, resume markers and archive contract unchanged unless explicitly requested.
5. Make changes-only ZIPs rooted at the project root. Do not include build/, .gradle/ or output/.
6. Run real tests/benchmarks and say exactly what passed or failed. A syntactically valid Bash script is not proof of equivalent results.
7. Do not delete historical scripts, weight files or canonical inputs without explicit approval. Provide candidate paths and exact cleanup commands, not destructive actions concealed in a patch.
