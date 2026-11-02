# Task 5B.9B — Draw, replacement, and upgrade forecasting

## Implementation

`ExpectedDrawProjection` is a side-effect-free shared helper for estimating sequential dice draws. It consumes the lowest-sided available Supply dice first, refilling from Discard once Supply is empty. Every unknown roll is represented by its arithmetic mean `(sides + 1) / 2.0`; no RNG or Roll Rewards are evaluated in predictions.

`CardScoringHelpers` now uses the shared forecast for **Queen's Blossom (Draw 2)**, **Reap What You Roll (discard 1, Draw 2)**, **Bursting Blossom (Raise 1, then Draw per maximum-valued die)**, and **Root Awakening (Upgrade and use now)**. Upgrade keeps its pre-existing bounded future-use component, while the immediate value estimates the new die's mean minus the old face.

Cultivation forecasts account for the Hand change and crossed available purchase tiers. Battle forecasts respect total available Strike capacity, and use the existing `BattleDiePlacementAnalyzer` for an estimated tactical contribution. The score is incremental relative to the removed die where applicable.

## Limitations / later follow-up

- Multiple Battle Draws are evaluated against the same snapshot; they do **not** yet optimize jointly across evolving Battle rows. This can overvalue repeated placement in one row. `forecast.projectedPlacementCount` applies the total capacity limit but individual tactical estimates are independent.
- When replacing a die during Battle, the existing analyzer still observes the die being removed; its row contribution is approximated rather than accurately removed first. This requires a stateful *prediction-only* Battle board or a corresponding exclusion-aware analyzer for full fidelity.
- Purchase tiers use aggregate purchasing power. Since purchases spend individual dice rather than a divisible pool, tier reachability is a directional estimate, not a complete Buy-plan optimizer.
- Unknown rolled values are arithmetic means. No probability sampling or predicted Roll Rewards occur.
- Future persistent upgrade value remains a bounded heuristic; it has not been replaced by a model of all future draws.
- These changes do not alter actual rules or experimental CSVs.

## Verification

Run `./gradlew test --tests '*ExpectedDrawProjectionTest'`, then `./gradlew test integrationTest simulationTest`. Tests were not confirmed to pass in the authoring environment.
