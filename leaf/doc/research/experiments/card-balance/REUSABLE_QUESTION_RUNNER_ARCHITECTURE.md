# Reusable Question Runner — Architecture and Compatibility Boundaries

**Status:** Complete as a documentation/architecture inventory, 2026-11-02. **Next:** A1.2 declarative specification and validator. **Evidence:** inspected source archive `leaf(20261102-215653).tgz` (archive has no `.git` directory; HEAD and working-tree status cannot be certified here). This is **not** an execution or statistical acceptance result.

## Scope and decision

Keep the existing rules engine, legal-Grove checking, Buy learner, research effect infrastructure, telemetry, and execution/checkpoint code. Introduce only an upper-level **question specification → strict validation/resolved conditions → stage execution adapters → common evidence/report** boundary. The question layer owns identity, experimental intent, controls, stages and provenance; existing runners own actual games, learning and mechanic execution. No new generalized game engine, strategy policy, or autonomous effect generator is authorized by A1.

A1.1 changes documentation only; existing historical CLI, output and seed contracts are **frozen** pending specific A1.2–A1.8 migrations and parity tests. Never relabel historical results under a new schema without an explicit adapter/version.

## Current implementation map

| Role | Existing implementation | Decision / limitation |
| --- | --- | --- |
| Canonical inputs and fingerprint audit | `bin/r0_audit_inputs.py`; official and resync CSVs under `data/` and `data/research/` | **Retain/wrap** input identity, source checksums and legal market validation; do not rewrite data sources. |
| Initial paired Sapping comparison (R1) | `bin/r1_sapping_compare`; `src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/card/R1SappingComparisonMain.kt` | **Historical adapter/reference**: fixed nine-card Grove, Sapping wording/slot guards and `original`/`different-die` conditions. Preserve its paired checkpoints and report contracts. |
| Targeted/unrestricted Sapping search (R2) | `bin/r2_sapping_discovery`; `.../card/R2SappingDiscoveryMain.kt` | **Historical/reference**: Buy-only coevolution and held-out score, with Sapping-targeted fitness. Do not generalize R2 by string replacement. |
| General nine-slot Plant selection and Buy discovery (R3A) | `bin/r3a_plant_screen`; `.../card/R3APlantScreenMain.kt` | **Core engine to wrap**: variable target ID, strict 9-slot Grove, training and evaluation, targeted/unrestricted roles, telemetry. `effect-variant` dispatch remains card-specific. |
| Calibration condition matrix (R3B) | `bin/r3b_effect_sanity_gate` importing `bin/r3a_plant_screen` | **Retain as compatibility/reference**: hard-coded `CASES`, official/disabled/extreme modes, independent searches and frozen-policy crossings. Generalize *above* existing mechanisms. |
| One-seat effect isolation (R3B.1) | `bin/r3b1_effect_isolation` importing R3B/R3A | **Retain/wrap**: official frozen specialists, opponent untouched, paired rows, mechanical-output assertions, resumption and Downloads publishing; hard-coded conditions remain historical. |
| Research-only Plant intervention dimensions | `.../experiment/plant/PlantExperimentConfig.kt`, `PlantExperimentConfigLoader.kt`; corresponding `src/simulationTest/.../plant/*Test.kt` | **Retain** immutable optional `type`, `cost`, `available`, `scoringRule`, `effect` overrides keyed by canonical Plant ID. Loader consumes `card_id,type,cost,available,scoring,effect` CSV (some optional); never modify canonical CSV for trials. |
| Per-seat experimental effect | `R3APlantScreenMain.kt`, `GameConfig.researchPlantEffectsBySeat` usage | **Retain** targeted intervention while opponents remain canonical. Verify role-to-seat mapping with tests; no global substitution for one-seat comparisons. |
| Development and mechanical output evidence | `R3APlantScreenMain.kt` `EVAL` header and its game-record construction, `game.researchEffectOutputs`; `doc/research/experiments/card-balance/EFFECT_VALUE_ISOLATION.md` | **Retain/export** named purchases, grafts, dice, target resolutions and immediate VP / die pips / drawn dice+ pips. These are *not* general causal attribution or full D1–D10 decision-event telemetry. Inventory D1–D10 separately before promising inclusion in generic output. |
| Long-run execution architecture | `doc/engineering/SIMULATION_LAUNCH_ARCHITECTURE.md`; `bin/lib/run_simulation_java`; `./gradlew --no-daemon writeSimulationRuntimeClasspath` | **Freeze** compile once, direct Java per independently isolated condition, persistent JVM *within* each run as supported. Never launch Gradle for every game. |
| Checkpoints, result artifacts | R1/R2/R3A/R3B/R3B.1 launchers; `doc/engineering/experiment-checkpointing.md` | **Freeze** each launcher’s existing manifest schema, fingerprint, hash/marker validation, completed-chunk reuse, failure propagation and `.tgz` packaging. Add new A1 versioned contracts instead of altering old ones silently. |
| Historical research documentation | `doc/research/experiments/sapping-snapdragon/*`; `doc/research/experiments/card-balance/{EFFECT_SANITY_GATE,EFFECT_VALUE_ISOLATION,EFFECT_AWARE_DECISIONS,PLANT_EFFECT_VARIANTS_SPEC}.md` | **Retain** as provenance and parity references. |

## Specific non-generic assumptions and migration targets

1. `R3APlantScreenMain.Options` accepts `official`, `disabled`, `root-raise20`, `queen-draw4`, `research-vp20`; code explicitly restricts Raise+20 to `Root_05_02` and Draw4 to `Flower_17_04`, then dispatches by name to `GameEffect` constants. `bin/r3a_plant_screen` repeats these guards. **A1.3:** replace this only on the new path with validated `condition → executable effect` resolution; keep old CLI interpretations working.
2. R3B has `CASES=(('Root_05_02','root-raise20'),('Flower_17_04','queen-draw4'))` and `MODES=('official','disabled','extreme')`; R3B.1 adds a one-sided named control matrix and checks +20 VP. **A1.4–A1.5:** describe stages/conditions as data, not hard-coded card pairs.
3. R1/R2 have embedded Sapping canonical wording, `Flower_11_03`/cost-11 checks, Grove, `original` versus `different-die`, and Sapping-specific report keys. **Do not** treat these as generic APIs or make a generic runner invoke their bespoke rules without an explicit adapter.
4. R3A's `EVAL` TSV schema is fixed and includes matchup/sample/role/seat, results, named development and mechanical outputs; its train scores schema differs. Historical R1/R2/R3B/R3B.1 reports use different schemas and assertions. **A1.6:** make a new versioned generic envelope and explicitly map each source, preserving raw files.
5. The present per-effect output observer captures a limited mechanical subset. It cannot certify resource changes, injuries, chained targets, end-to-end opportunity costs or dominance for every effect. **A2 limitation**, not a reason to delay A1.2.
6. R3A learns Buy; the non-Buy decisions remain Human Baseline. Any generic run must state these policy scopes clearly, especially when presenting apparent negative evidence about strategic effect use.

## Contracts to freeze before A1.2

- **Inputs:** current official/resync data, all nine *legal* Grove slots, exact target card identity, full effective wording, and fingerprints; target must be present. Current research default round structure is 3/2/2; canonical versus experimental Battle rules must be explicit in a future question spec.
- **Isolation:** baseline/candidate changes must be intentional, per-seat changes must not leak to opponents, frozen weights must not be overwritten by training, training-only target incentives must not appear as held-out success bonuses.
- **Randomness:** record mechanical, strategy/evolution and holdout seeds; a paired starting schedule is not a promise of identical post-divergence RNG draws.
- **Runtime:** one build before multiple direct-Java condition processes; keep independent mutable condition/policy processes isolated and in-process game batches persistent where documented.
- **Resume integrity:** manifest mismatch fails, incomplete/invalid chunks are rejected, successful chunks can be reused only after checking required fingerprints/row counts/schemas. Do not silently tolerate absent cohorts.
- **Outputs:** raw historical TSV/JSON, reports and checkpoint artifacts remain under their old names/headers; create a separately versioned new `question-*` manifest and report in A1.4/A1.6. Preserve full counts/uncertainty, incomplete status, and provenance. One validated `.tgz`, copied to `~/Downloads` where supported, prints `UPLOAD THIS FILE:`; changed-files installation assumes ZIP from `~/Downloads`.
- **Claims:** unit/operational PASS is not evidence of card balance; absent usage or missing countersearch means *Investigate*. Historical results cannot be pooled across definitions or policy fingerprints without validation.

## Responsibility boundary for A1.2 onward

```text
Question specification (new; declarative; no executable card logic)
  → validator / resolved identity, Grove, rules, effect registry, stages and seeds (new)
  → condition and role planner (new; versioned plan)
  → adapters to R3A Buy train/evaluate, R3B frozen comparison, R3B.1 seat isolation (thin)
  → existing GameFactory / GameRunner / effect resolution / trained policies (unchanged)
  → raw historical evidence + new common versioned per-question reporting
```

**Ownership rule:** a question file may select an *already implemented/tested* executable effect; it may not describe arbitrary text and assume a new mechanic is executable. Price/type/scoring variants must remain compatible with legal Grove construction and cannot bypass the canonical 9-slot validation.

## A1.2 starting point and acceptance evidence

Implement a small format/strict resolver, **not** a broad DSL. Start with two unrelated reference questions (Root Four More and Sapping Snapdragon). Include ID/definition fingerprints, fully enumerated Grove and rules/policy context, explicit effect condition names/audience, deterministic seeds and stages. `--plan` must print the effective values and fail on stale IDs/definitions and illegal slots. Subsequent A1.3 work binds conditions to existing executable overrides. Keep legacy launchers untouched until A1.7 matched parity tests.

## Verification performed for A1.1

- Extracted and read the supplied archive's `README.md`, `TASKS.md`, launch architecture, relevant Python launchers and Kotlin experiment/config sources.
- Confirmed the inspected paths and hard-coded guards by direct source inspection. No source/data/policy files modified.
- No Gradle build, simulation, or unit test was run; not needed to assert this documentation-only inventory. Archive lacks `.git`, so actual commit SHA and dirty state are **unknown** and must be captured in the user's checkout before A1.2 research runs.
