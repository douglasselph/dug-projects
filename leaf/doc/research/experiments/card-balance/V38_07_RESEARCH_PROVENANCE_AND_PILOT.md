# V38-7 — Ruleset provenance and reusable Plant-question execution pilot

**Engineering status:** Implementation ready for Ubuntu JVM verification. Do not mark the two-question v38 cohort complete until a successful uploaded pilot archive has been reviewed. This is not a card-balance certification.

## Why this work is necessary

The adopted v38-01 Battle rules resolve, Doom and clean each Strike separately. Older research cohorts used `LEGACY_WHOLE_BATTLE`. Replacing an old cohort's label with `THREE_ENGAGEMENTS` would be invalid even if the Plant CSV definitions matched. The generic Plant question planner understood both rule names, but the old R3A training/evaluation implementation previously hard-coded legacy in `GameConfig`.

## Changed boundaries

- `bin/lib/research_ruleset_provenance.py` emits a content-addressed research record. It hashes **the v38-01 rulebook**, four canonical card CSVs, three research/rules configuration inputs, the main/simulation Kotlin source trees, relevant Python launchers/condition resolvers and Gradle build files. The schema, exact `battle_structure`, round pattern and ruleset input hashes are included in the identity. Git revision and dirty status are recorded as supplementary audit information, *not* part of the reproducible input fingerprint. Missing source inputs fail closed; pre-v38/unknown manifests cannot be silently treated as certified v38 provenance.
- `R3APlantScreenMain.Options` now accepts `--battle-structure=THREE_ENGAGEMENTS|LEGACY_WHOLE_BATTLE`. The old default remains `LEGACY_WHOLE_BATTLE` strictly for historical direct-script compatibility. Both Buy training and held-out evaluation use the **selected** structure in `GameConfig`. Unknown `CANONICAL` remains invalid.
- `bin/r3a_plant_screen` accepts the same explicit selector and passes it to **each** direct-Java JVM. Ruleset provenance is incorporated in the manifest, run ID, and stage/checkpoint fingerprints. Historical cohort data is not rewritten. Independent runs remain independent JVM processes, and only the first R3A compilation launches Gradle.
- The reusable `bin/plant_question` launcher now passes the **question's** requested ruleset to R3A instead of rejecting the adopted rules. Plans include stable ruleset provenance; stage output directories are namespaced by Battle structure. Generic official/suppressed paired and adapted Buy/countersearch adapters pass the same ruleset through their existing Kotlin execution path. `--plan` still never executes games.
- Two small v38 question specimens use the same general runner without special code for their identities: `questions/root-four-more-v38.json` and `questions/sapping-snapdragon-v38.json`. These are deliberately **tiny correctness pilots** (one generation, three candidates, four training games/candidate, eight held-out games/matchup). They cannot support balance verdicts.

The old archived studies (R1/R2/R3A/R3B/R3B.1) are **not upgraded** by this patch. Existing legacy question files remain explicitly `LEGACY_WHOLE_BATTLE`. Catalog definitions and effect semantics were **not changed**.

## Verification: one command on Ubuntu

From the `leaf/` repository root, after extracting the changed-files ZIP **from `~/Downloads`** into the parent directory:

```bash
bin/verify_v38_question_runner --plan-only
bin/verify_v38_question_runner |& tee ~/Downloads/leaf-v38-7-question-runner.log
```

The full verification runner:

1. Validates the two v38 plans and distinct Plant targets.
2. Runs Python tests for the question pipeline and provenance.
3. Runs selected Kotlin R3A option tests while compiling the simulation runtime **once** (`writeSimulationRuntimeClasspath`).
4. Runs **both** Plant questions with `--run-paired-frozen --no-build` and `--run-adapted-countersearch --no-build` through independent direct-Java JVMs; the latter requires the former's local frozen-weight results.
5. Preserves all logs, completed result `.tgz` files, source/data/ruleset fingerprints, stage results, failures and timing in a **single verification `.tgz`** copied to `~/Downloads`, printing `UPLOAD THIS FILE:` whether it passed or failed (except catastrophic preflight failure).

An archive whose `verification.json` status is `PASS_TWO_V38_QUESTION_COHORTS_EXPLORATORY_ONLY` proves that the generic *engineering path* executed twice. It does **not** prove a strategy is expert-strength, an effect's value was isolated for every mechanic, or that its confidence intervals are adequate.

The full pilot is not runnable in the current portable environment because Gradle 8.13 is not available there. Until Ubuntu results arrive, treat the Kotlin routing and experiment-stage integration as **unverified**, regardless of passing Python/plan tests.

## Next gates after pilot passes

1. Confirm the per-question reports contain the expected target/Grove, ruleset fingerprint and distinct official/suppressed mechanical evidence; verify no control leakage.
2. Close the **v38-capable two-question functional slice** (part of A1.4/A1.5/A1.7), leaving unsupported player counts, price variations and broader effect conditions explicit.
3. Broaden the common evidence/report schema (A1.6). Do not derive any all-36-card conclusions from eight held-out matches.
4. When the revised Plant card effects arrive, verify official CSV → executable `GameEffect` semantics and card-target legality before running meaningful balances. Fingerprints will intentionally change and invalidate old research comparisons.
