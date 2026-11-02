# Documentation Map

Use the repository-level [`README.md`](../README.md) as the primary entry point and [`../TASKS.md`](../TASKS.md) as the current backlog.

The `doc/` directory contains both **current governing documentation** and **historical implementation/research records**. A file's presence here does not by itself mean it is current or that its task remains open.

## Read these first

For a fresh coding/research conversation, normally read:

1. [`../README.md`](../README.md) - purpose, governing design goals, current state, sources of truth and workflow requirements.
2. [`../TASKS.md`](../TASKS.md) - active work, deferred work and priorities.
3. [`LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md`](LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md) - research philosophy, evidence standards, card effect/pricing distinction and reusable experiment direction.
4. [`SIMULATION_LAUNCH_ARCHITECTURE.md`](SIMULATION_LAUNCH_ARCHITECTURE.md) - compile-once/direct-Java/persistent-JVM architecture, isolation and operational contracts.
5. [`SIMULATION_OPERATIONAL_READINESS.md`](SIMULATION_OPERATIONAL_READINESS.md) - what the operational gate does and does not establish.
6. The task-specific document(s) for the current investigation.

## Current task-specific front doors

### Battle pacing / Candidate B

- [`BATTLE_PACING_RESEARCH_STATUS.md`](BATTLE_PACING_RESEARCH_STATUS.md) - consolidated purpose, current implementation status and remaining evidence gaps.
- [`BATTLE_STRUCTURE_CONFIGURATION.md`](BATTLE_STRUCTURE_CONFIGURATION.md) - current `game-rules-current.properties` selection behavior.

The many `BATTLE_PACING_CHECKPOINT_*`, `BATTLE_PACING_POLICY_TASK*` and `TASK7_*` files are detailed implementation/provenance records. Read them when working on those exact layers; do not treat each as a competing current roadmap.

### Human Baseline

- [`HUMAN_BASELINE.md`](HUMAN_BASELINE.md) - Human Baseline certification/specification map.
- [`HUMAN_BASELINE_BATTLE.md`](HUMAN_BASELINE_BATTLE.md) - Battle behavior and completed B-series work.
- [`HUMAN_BASELINE_CULTIVATION.md`](HUMAN_BASELINE_CULTIVATION.md) - Cultivation behavior.
- [`HUMAN_BASELINE_POLICY.md`](HUMAN_BASELINE_POLICY.md) - shared scoring/tuning policy.
- [`HUMAN_BASELINE_DECISION_CALIBRATION.md`](HUMAN_BASELINE_DECISION_CALIBRATION.md) - decision calibration context.

Detailed C01-C05 documents are retained as incremental engineering records. Current policy is to **defer broad fine calibration** unless a concrete research question is blocked; see `TASKS.md`.

### Card/effect research

- [`R0_CORRECTNESS_AUDIT.md`](R0_CORRECTNESS_AUDIT.md)
- [`R1_SAPPING_QUESTION_EXPERIMENT.md`](R1_SAPPING_QUESTION_EXPERIMENT.md)
- [`R2_SAPPING_STRATEGY_DISCOVERY.md`](R2_SAPPING_STRATEGY_DISCOVERY.md)
- [`R3A_ALL_PLANT_SCREENING_FOUNDATION.md`](R3A_ALL_PLANT_SCREENING_FOUNDATION.md)
- [`R3B_EFFECT_SANITY_GATE.md`](R3B_EFFECT_SANITY_GATE.md)
- [`R3B1_EFFECT_VALUE_ISOLATION.md`](R3B1_EFFECT_VALUE_ISOLATION.md)
- [`R3B1A_EFFECT_AWARE_DECISIONS.md`](R3B1A_EFFECT_AWARE_DECISIONS.md)

These are useful implementation/evidence records. The current general direction is in `LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md` and `TASKS.md`: reusable arbitrary-card questions, effect-vs-purchase/pricing separation, stronger effect awareness, and eventually the all-36-card audit.

## Architecture and operation references

- [`USING_THE_SIMULATOR.md`](USING_THE_SIMULATOR.md) - practical source-set and experiment workflow.
- [`CODE_ARCHITECTURE.md`](CODE_ARCHITECTURE.md) - deeper programming architecture.
- [`TESTING_AND_VERIFICATION.md`](TESTING_AND_VERIFICATION.md) - test responsibilities and commands.
- [`CHRONICLE_AND_OUTPUT.md`](CHRONICLE_AND_OUTPUT.md) - Chronicle/output conventions.
- [`EXPERIMENT_COOKBOOK.md`](EXPERIMENT_COOKBOOK.md) - experiment construction patterns; some examples are historical, so reconcile with current task docs.
- [`BIN_COMMANDS.md`](BIN_COMMANDS.md) - designer-facing command reference.
- [`PYTHON_RESEARCH_TOOLING.md`](PYTHON_RESEARCH_TOOLING.md) - Python-side tooling.

## Research/data provenance references

Documents such as A02-A06, D2-D10, market reports, Round/Sunlight provenance, effect variants and experiment-specific reports are retained because they explain important boundaries, historical inputs and prior evidence. Use them when relevant to a concrete task rather than reading them all as one current plan.

## Historical roadmaps and handoffs

The following kinds of documents are **historical context**, not the current front door:

- `SIMULATION_PLAN.md` - early Milestone-3/Six-Wisp-era roadmap.
- `LEAF_RESEARCH_HANDOFF.md` - older stand-alone handoff; superseded by repository `README.md` + `TASKS.md` + current governing docs.
- older one-off experiment plans/results whose work has since been completed or superseded;
- older rulebook snapshots such as `Leaf_and_Let_Die_v36-01.pdf`.

Keep them for provenance unless deliberately archiving/removing them with Git history available. Do not let them override current source, current data, `README.md`, `TASKS.md`, or a newer task-specific status document.

## Rulebook note

The current tabletop rules reference for research is [`Leaf_and_Let_Die_v37-03.pdf`](Leaf_and_Let_Die_v37-03.pdf) unless a newer approved rules source is supplied. `Leaf_and_Let_Die_v36-01.pdf` is historical.

## Source-local README files

Some documentation intentionally lives beside the code it describes:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/README.md
src/integration/kotlin/dugsolutions/leaf/integration/v35/README.md
src/simulation/kotlin/dugsolutions/leaf/simulation/v35/README.md
```

Use those when navigating the corresponding package/source set.
