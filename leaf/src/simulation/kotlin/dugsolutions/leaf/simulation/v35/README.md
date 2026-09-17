# v35 Simulation Layer

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
