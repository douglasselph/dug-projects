# v35 Simulation Layer

This source set answers the research question:

> **What patterns emerge when trusted Leaf & Let Die rules and strategies play many complete games?**

It is deliberately different from `src/integration`, which constructs exact deterministic scenarios to verify that the real engine behaves correctly. Simulation code measures tendencies; integration code proves exact behavior.

The canonical **Human Baseline implementation does not live here**. It intentionally lives in production code at:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/
```

This simulation layer consumes Human Baseline through `StrategyProfile.humanBaseline()` and adds research-specific strategies, modifiers, experiment runners, aggregation, and future learning infrastructure.

See [Human Baseline Specification](../../../../../../../doc/HUMAN_BASELINE.md) for the eight-area Milestone-2 certification map and [Testing and Verification](../../../../../../../doc/TESTING_AND_VERIFICATION.md) for the Unit → Integration → Simulation distinction.

The v35 decision/simulation architecture has distinct purposes:

1. **Mechanical Control (Level 0, core)** — deterministic, legal, deliberately
   naive behavior for engine tests and a bottom benchmark.
2. **Human Baseline (Level 1, core)** — the canonical simulation control group,
   intended to model simple, reasonable experienced-human behavior. It now has
   immutable DecisionContext views, shared heuristics, card scorers, global
   influences, complete candidate scoring, strategy-only tie RNG, and optional
   score reasoning.
3. **Planned Baseline (simulation modifier)** — Human Baseline plus exact desired
   Plant-copy counts. It modifies only Buy priorities; all other decision areas
   remain Human Baseline. This is a composable research dimension, not a higher
   strategy level.
4. **Tactical (Level 2, simulation)** — short-horizon improved play.
5. **Strategic (Level 3, simulation)** — broader plans/opponent-aware play.
6. **Learned/Adaptive (Level 4, simulation)** — trained/persisted behavior.

Mechanical game randomness and strategy tie-breaking randomness are separate
streams. An experiment can therefore hold mechanical seeds fixed while varying
strategy tie seeds, or reproduce both streams together.

## Planned Baseline example

Use stable Plant-card `name` keys from the CSV data, not display titles:

```kotlin
val plan = CreaturePlan.of(
    "Root_07_02" to 2,     // Root Appreciation
    "Flower_14_01" to 1,  // Bee-loved Bloom
    "Flower_17_04" to 1   // Queen's Blossom
)

val profile = StrategyProfile.plannedBaseline(plan)
```

While a target is still unmet, the ordinary Human Baseline purchase score gets
+40 for its first desired copy or +30 for an additional desired copy. Once the
exact requested count is owned, the plan contributes no further bonus.

Integration-test scripted strategies remain under `src/integration` and should
normally fall back to **Mechanical Control**, never Human Baseline. This keeps
engine tests deterministic as Human Baseline evolves.

## Focused-card experiment harness

`CardFocusExperiment` runs one Planned Baseline player against ordinary Human
Baseline opponents and rotates the focused player through every seat. The same
sample seed is reused for each seat rotation, while mechanical and strategy
seed streams remain separate.

```kotlin
val experiment = CardFocusExperiment(
    gameFactory = gameFactory,
    gameRunner = gameRunner
)

val result = experiment.run(
    spec = CardFocusExperimentSpec(
        targetCard = TargetCard("Root_07_02"),
        targetCount = 2,
        numPlayers = 4,
        gamesPerSeat = 1_000,
        baseSeed = 10_000L
    ),
    selectedPlantCards = selectedPlantCards
)

println(CardExperimentReport.render(result))
```

A four-player `gamesPerSeat = 1_000` run executes 4,000 total games. Results
include focused-vs-baseline win share and VP deltas, acquisition frequency,
copies purchased/surviving, target-card activations, target Plant VP, Battle
Strike VP, and a per-seat breakdown. The result is intentionally aggregate data
rather than retained Games/Chronicles so high-volume batches stay lightweight.

## Baseline calibration before exploit experiments

Before interpreting an intervention such as Six-Wisp, run the same exact
nine-card Grove with four ordinary Human Baseline players and establish the
ordinary seat/noise behavior. The calibration API should accept the selected
Plant-card list and requested game count/checkpoints as explicit inputs.

Keep two layers separate:

- a small deterministic regression proves reproducibility, seed sensitivity,
  compact Plant/dice signatures, and aggregation correctness;
- a manual long-run calibration reports cumulative seat win share at increasing
  sample sizes (for example 100, 250, 500, 1,000, 2,000) so the designer can
  see when the four seats settle near the neutral 25% reference.

Do not assert that four games must have four different winners, and do not put a
large statistical fairness run in normal `integrationTest`. Both would confuse
random variation with a software invariant. Long-run calibration is research
output. Use the same Grove and matched seed schedule for the later intervention
so its delta can be compared with ordinary baseline variation.
