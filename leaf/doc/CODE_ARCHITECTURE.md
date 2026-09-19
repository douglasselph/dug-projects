# Code Architecture

This document is the deeper programming-oriented map of the v35 codebase. Read it when you need to understand where a change belongs or how the simulator is assembled.

## 1. Architectural boundary

The core split is:

```text
main          = rules engine + decision API + production strategy behavior
unit test     = isolated correctness of main
integration   = deterministic real-engine scenarios
simulation    = strategy research and repeated-game experiments
simulationTest= tests of simulation/research code
```

The most important discipline is to keep experiment-specific concerns out of the rules engine while still giving the research layer enough stable, immutable information to make meaningful decisions.

---

## 2. Repository-level layout

```text
build.gradle.kts
settings.gradle.kts
data/
src/
  main/
  test/
  integration/
  simulation/
  simulationTest/
doc/
```

`data/` contains card CSV data and image assets used by the project. Production code accesses card data through the v35 data-loading layer (notably `common/CardDataFiles`).

---

## 3. Production engine: `src/main`

The v35 root package is:

```text
dugsolutions.leaf.v35
```

### `game/`

Top-level game lifecycle and orchestration.

Important types include:

- `Game`
- `GameConfig`
- `GameRunner`
- `GameStatus`
- `GameFactory`
- buy/operation/scoring helpers beneath the package

`GameConfig` is especially important to simulation work. It carries the selected Plant cards, per-player decision factories, Round setup, mechanical seed, strategy seed, die-factory configuration, and optional decision-reasoning recording.

### `round/`

Round cards, deck/manager/registry, and round execution/coordinator logic.

This layer decides what kind of round is active and dispatches the corresponding real Cultivation/Battle behavior.

### `battle/`

Battle domain and resolution:

- Battle ordering/placement;
- grid/column/square/row state;
- Strike resolution;
- Doom;
- Battle-specific state.

### `player/`

Player state, dice, Wisp hand, factory wiring, and the decision subsystem.

The decision package is one of the most important parts of the simulation architecture.

### `plant/`, `wisp/`, and card/round domains

Card models, registries, stacks/decks/managers, and card-specific mechanics.

### `effect/`

Generic effect model, conversion, routing/execution, handlers, and special effects.

This is the bridge between card text encoded in the data model and concrete game-state changes.

### `grove/`, `tokens/`, `creature/`

Shared supplies, graft topology, Plant Creature state, and physical resource models.

### `chronicle/`

Structured record of game events.

Typed Chronicle entries are useful for:

- deterministic assertions;
- debugging;
- statistical instrumentation;
- future training datasets;
- human-readable rendering without coupling tests to prose.

### `random/`

Mechanical randomization and die abstractions.

Mechanical RNG is intentionally separate from strategy tie-breaking RNG.

---

## 4. Decision architecture

The decision system allows the rules engine to request a player choice without hard-coding one universal AI.

### Decision areas

`DecisionDirector` coordinates independent decision areas such as:

- Cultivation;
- Battle;
- Buy;
- Support;
- Effect choices;
- Wounds;
- Roll Rewards;
- Creature placement.

This allows experiments to replace one dimension while leaving the rest at a shared baseline.

### Mechanical Control

The deterministic Level-0 policy exists primarily for engine control/testing. It plays legally but is deliberately naive.

Integration scripts normally fall back to Mechanical Control so evolving Human Baseline heuristics do not destabilize deterministic correctness tests.

### Human Baseline

The Level-1 Human Baseline is the canonical simulation control group. It is intended to represent simple, reasonable experienced-human decisions rather than optimal play.

Its current implementation is decomposed under:

```text
player/decision/baseline/
```

Important subareas include:

```text
battle/
buy/
card/
common/
context/
cultivation/
effect/
influence/
placement/
reward/
scoring/
support/
wound/
```

Shared heuristic classes currently include concepts such as:

- die value and reroll expectation;
- purchase thresholds;
- resource reserves;
- `RowNeed`;
- graft topology;
- Creature/dice development targets.

The scoring framework includes:

- `PriorityScore`
- `ScoreAdjustment`
- `ScoredChoice`
- `ScoreExplanation`
- `BaselineScoreEngine`

This structure is useful because a decision can be explained in terms of reusable adjustments rather than hidden inside one giant strategy method.

### Decision context

Strategies consume immutable decision context/view data rather than receiving unrestricted mutable access to the entire `Game`.

That boundary is important for both correctness and future learning work: a strategy should observe what it is allowed to know and return a choice; the rules engine remains responsible for mutation.

---

## 5. Randomness model

The codebase intentionally separates two categories of randomness.

### Mechanical randomizer

Used for game events such as:

- dice rolls;
- deck shuffles;
- effects whose rule is genuinely random.

Configured through `GameConfig.seed`.

### Strategy randomizer

Used for equal-score/tie decisions within strategy logic.

Configured through `GameConfig.strategySeed`, with an independent per-player derived stream.

Why this matters:

```text
Strategy A and Strategy B
        ↓
play the same mechanical game stream
        ↓
strategy changes do not accidentally alter later dice/deck randomness
```

This is foundational for paired simulation experiments.

---

## 6. Integration architecture: `src/integration`

The integration source set is test-only code that uses the real production engine.

### Harness

`IntegrationGameHarness` provides access to the constructed game, round coordinator/runner, snapshots, and Chronicle.

### Scenario description

`GameScenario` describes the controlled scenario used by a test.

### Deterministic randomness

`ScriptedRandomizer` supplies exact random results and can fail if unexpected randomness is consumed.

### Scripted decisions

`ScriptedDecisionDirector` and its area-specific strategies queue/select exact choices from the legal options offered by production code.

A key design principle is that scripts select **legal production choices** rather than constructing fake actions that bypass rule validation.

### Snapshots and assertions

Reusable immutable snapshots and Chronicle assertions make cross-subsystem tests readable and resilient.

Integration code should not become a second implementation of the game. It should control inputs, execute production code, and observe outputs.

---

## 7. Simulation architecture: `src/simulation`

The simulation source set is a research application layer on top of `main`.

### `strategy/`

Contains named strategy profiles and higher-level research strategy families.

`StrategyProfile` associates a readable strategy name, a classified strategy level for each decision area, and a fresh `PlayerDecisionFactory` for every player/game.

Current profile entry points include:

- `mechanicalControl()`
- `humanBaseline()` / `baseline()`
- `plannedBaseline(...)`

The Planned Baseline is intentionally a modifier rather than a new “intelligence level”: it layers an exact Plant acquisition plan over the Human Baseline Buy priorities.

### `experiment/`

Contains generic research concepts:

- `ExperimentConfig`
- `Matchup`
- `Tournament`
- `BatchRunner`

`BatchRunner` is currently an interface/boundary rather than a complete general-purpose batch implementation.

### `experiment/card/`

The first substantial concrete experiment family.

`CardFocusExperiment`:

- builds a Planned Baseline focused on one target card/count;
- pits it against Human Baseline opponents;
- rotates the focused strategy through every seat;
- reuses sample seeds across seat rotations;
- runs complete games through `GameFactory` + `GameRunner`;
- aggregates results rather than retaining all game objects/Chronicles.

This is the pattern to copy for other focused studies.

### `analysis/`

Aggregate result/summary types belong here rather than in the core rules engine.

### `learning/`

The project already has boundary types for:

- features;
- models;
- training examples;
- persistence.

These are future-facing. The project should continue to get value from scripted/heuristic experiments before making learning systems the center of the design process.

---

## 8. Why `integration` and `simulation` are separate

The separation prevents two common mistakes.

### Mistake 1: using statistical code to hide incorrect rules

If an exact Battle interaction is wrong, running it 100,000 times produces a very precise measurement of a bug.

Integration tests should prove the rule path first.

### Mistake 2: turning integration tests into research programs

A deterministic test should fail because an exact invariant changed. It should not fail because a win rate moved from 52.1% to 51.8%.

Simulation experiments need aggregate metrics, uncertainty awareness, paired samples, seat rotation, and potentially long runtimes. Those concerns do not belong in engine sanity tests.

---

## 9. Where to place a new change

| Change | Location |
| --- | --- |
| New game rule or domain behavior | `src/main` |
| New canonical decision API/context | `src/main/.../player/decision` |
| New Human Baseline heuristic used by all research | `src/main/.../decision/baseline` |
| Exact scripted scenario control | `src/integration/.../support` |
| Deterministic cross-subsystem correctness test | `src/integration/.../sanity` |
| New research strategy/modifier | `src/simulation/.../strategy` |
| New repeated-game experiment | `src/simulation/.../experiment` |
| Aggregation/reporting for research | `src/simulation/.../analysis` or experiment-specific package |
| Tests for experiment/config/aggregation | `src/simulationTest` |

---

## 10. Data flow of a simulation game

At a high level:

```text
Experiment specification
        ↓
StrategyProfile(s)
        ↓
PlayerDecisionFactory per seat
        ↓
GameConfig
  ├─ selected Plant cards
  ├─ Round setup
  ├─ mechanical seed
  └─ strategy seed
        ↓
GameFactory
        ↓
Game + real production rules engine
        ↓
GameRunner
        ↓
completed result + Chronicle/game state
        ↓
experiment-specific observation
        ↓
aggregate result/report
```

The experiment should observe only the information it needs and, for high-volume work, avoid retaining complete Games or Chronicles unless the study specifically needs them.
