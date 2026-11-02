# Phase A — calibrated decision scoring foundation

The current `PriorityScore` is already the common comparison scale. Phase A adds
`DecisionScoreBuilder`, named fixed/range influences, immutable samples, and
separate configuration loaders. These are **opt-in infrastructure**. Existing
Human Baseline policy behavior and learned weights are unchanged until Phase B.

## Calibration: existing Compost weights

Source: `CompostPriority.kt`. Current fixed score contributions are base +75,
per upgrade side +2, per remaining Cultivation round +2 (maximum 5 rounds),
per crossed Buy threshold −10 (as returned by the threshold heuristic),
low remaining Hand Buy power −40, and development deficit bonus 0..9 from
`HumanBaselinePolicy`. The no-upgrade-target fallback is 15.

The legacy *willingness check* is **not** a score weight: target showing 1/2
uses 75%, other rolls 50%, too-low remaining Hand buying power 10%, and
second commitment halves the percentage. On decline a −100 score penalty is
applied. Probability of *selecting* Compost depends on the margins over the
other legal actions. A faithful replacement needs score-margin distributions,
not a numeric reinterpretation of '75%' as '+75'.

For Phase B, collect for each legal Compost opportunity: pre-gate Compost score,
best competing score, their difference, gate percentage/result, Compost variant,
target die, action index, and winner. Estimate score-range adjustments against
these observed margins, then validate **observed selection rates and final VP**
under legacy vs new scoring on identical seeds. A range is a random *score
influence* drawn once from strategy RNG, **not** a selection probability.

## Randomness

The engine already seeds each player's `StrategyRandomizer` separately in
`GameFactory`; this is distinct from the mechanical `Randomizer`. Phase A
uses only `StrategyRandomizer` for sampled influences. Event-keyed mechanical
replay belongs to Phase C.

## API example

```kotlin
val builder = DecisionScoreBuilder("compost", strategyRandomizer)
val score = builder
    .add(ScoreInfluence("compost.base", "Original baseline", ScoreValue.Fixed(75)))
    .add(ScoreInfluence("compost.low_roll", "Low showing target", ScoreValue.Range(0, 15)))
    // Edit compost.base in human-scoring.properties instead of external weights.
    .build()
println(score.explanation()) // Actual sampled values retained, no re-roll
val legacyComparableScore = score.toPriorityScore()
```

Configuration files: `data/ai/human-scoring.properties` (Human score overrides)
and `data/ai/decision-experiment.properties` (policy mode only). The scored
Human Cultivation policy reads these named values when explicitly selected.
Learned-mode weights and legacy Human scoring are unchanged.
