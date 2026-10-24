# Task 12 — incremental experiment-family migration

## Scope

This is an execution-launch migration, **not** a conversion of all learning experiments into a single long-lived JVM. Evaluation and training Kotlin Mains already process their requested game samples inside one invocation. Each invocation continues to start its own JVM. Training, frozen-policy evaluation, and policy-interaction **conditions remain isolated processes** to avoid cross-condition mutable policy state and simplify failure recovery.

- Nine `bin/evaluate_*` wrapper commands and eight `bin/train_*` wrapper commands call `bin/lib/run_simulation_task`.
- The dispatcher maps supported Gradle `JavaExec` tasks to their exact Kotlin `MainKt` class, and passes arguments as independent tokens (including paths with spaces).
- Default `LEAF_SIM_LAUNCH=java` uses the compiled simulation classpath and direct Java. `LEAF_SIM_LAUNCH=gradle` uses the previous `./gradlew -q TASK -Pargs="$*"` path.
- `LEAF_SIM_AUTO_BUILD=1` (default) builds once when the classpath manifest is missing. The manifest is not a freshness detector: after source changes run `./gradlew writeSimulationRuntimeClasspath` explicitly.
- `bin/experiment_policy_interactions` compiles once up front (except `--dry-run`) in Java mode and sets `LEAF_SIM_AUTO_BUILD=0` for all its separate condition JVMs. Everything else in the Bash experiment—input verification, condition seed matching, skip markers, logs, aggregation, and archive—is unchanged.
- Exact pre-migration copies of the 17 wrappers are in `bin/legacy/launchers/`; the old top-level wrapper command also remains runnable with the mode switch.

## Deliberately not migrated

Complex training loops, overnight multi-arm studies, custom Gradle init script crossplay, and miscellaneous nonstandard `-P...` task launchers retain their existing Gradle orchestration. They require separate reviews of state isolation, failure modes, input semantics, and output provenance before conversion. This patch does not assume their outputs are equivalent.

## Commands

```bash
./gradlew writeSimulationRuntimeClasspath
LEAF_SIM_LAUNCH=java bin/evaluate_battle_main_policy --help
LEAF_SIM_LAUNCH=gradle bin/evaluate_battle_main_policy --help
LEAF_SIM_LAUNCH=java bin/experiment_policy_interactions --dry-run --conditions A --samples 2
```

The last command is a Bash plumbing check (not a policy evaluation). For a real A/B confirmation, use the same seeds, Plant/Round inputs, frozen weights and isolated output directories in Java vs Gradle mode. Compare normalized result data and provenance, not timing or console banner text. **Do not promote policies** based solely on a launcher change.

## Verification scope

Static checks: Bash `bash -n`; all dispatcher mapping names match `build.gradle.kts` Main classes; legacy wrapper snapshots reproduce original task invocations. Full Gradle compilation and end-to-end policy evaluation should be run in the user's actual Ubuntu checkout. The presence of the benchmark harness does not by itself establish that the baseline speed/equivalence criteria were met.
