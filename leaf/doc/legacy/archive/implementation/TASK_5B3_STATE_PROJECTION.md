# Task 5B.3 — Immutable Human Baseline projection foundation

## Scope and status

Adds `baseline/projection/HumanStateProjection.kt` and a focused test class. This is deliberately **infrastructure only**: no existing decision policy is wired to it and no canonical/research card definition is changed. Task 5B.4–5B.7 will introduce effect-specific legal choice generation and decision integration.

## Contract

- `ProjectedState` is an evaluation snapshot, **not** the rules-engine game state.
- Callers obtain *legal choices* from existing effect request/decision code; the projection layer never grants actions or resolves them.
- `HumanStateProjection.apply()` is deterministic, pure and sequential: each step produces a new state, original inputs remain unchanged. No RNG, rolls, Roll Rewards, mutable game-engine components or hidden future knowledge.
- `ExpectedDraw(D6)` produces **3.5**, `D4` **2.5**, etc. An expected value is fractional and is never passed as a physical die face. No Roll Reward is credited for that representative mean.
- `RaiseDie` caps the gain at the die's maximum and successive projections use the already-updated value.
- `OwnStrikeDelta` / `OpponentStrikeDelta` model hypothetical totals without changing the Battle Grid; the caller supplies effects/targets and their legal consequences.
- `HumanProjectionEvaluator` converts affected rows to the existing `BattleSwingEvaluator`, with each affected row counted once; it reports *threshold crossings* based on existing `PurchaseThresholdHeuristics.tiers` and real-valued predicted buying power.
- No implicit additive scoring of raw die gain and row swing: the outcome separates Battle tactical valuation, purchasing power change, and thresholds. Later policy code must choose appropriate score units and avoid paying twice for the same consequence.

## Limitations before use in real policies

1. `buyingPower` plus affordable-tier crossings are diagnostics, **not** a complete legal multi-purchase planner; actual Buy splits dice and Critters into separate purchases. Tasks 5B.4 and 5B.10 must account for this.
2. `ProjectedStrike` tracks row totals, not placed individual dice, Strike Square capacities, card facings, resource counts or future opponent reactions. Callers must model all relevant consequences and legality before evaluating.
3. For newly drawn dice, treating the roll as the mean is a user-chosen approximation, *not* an exact expectation of nonlinear effects such as crossing a threshold. Do not manufacture a roll reward for this mean.
4. The Battle evaluator uses the existing policy's named transition weights. `ProjectedStrike` derives victory/tie/wound and live-threat margins from participant totals, but intentionally does not predict opponent future plays.
5. No current `HumanBaselineEffectStrategy`, `DrawPriority`, card scorer or action selector is altered in this checkpoint.
6. Input collections are Kotlin read-only List/Map/Set; external callers should provide stable read-only snapshots and must not mutate any externally retained mutable references. The projection operations themselves never mutate their collections.

## Tests

`./gradlew test --tests '*HumanStateProjectionTest'`

Covers mean draws, fractional purchasing power, threshold changes, cap-aware sequential raises, before/after multi-row Battle analysis, and projection branch isolation.

## Validation

The Gradle wrapper attempted to download Gradle 8.13 but network resolution for `services.gradle.org` failed. Tests **have not been compiled or executed here**. Run the command above and then `./gradlew test integrationTest simulationTest` in the user's development environment.
