# Task 5B.9D — Target/activation scoring alignment

## Changes

- **Bee theft:** `PlantResourceProjection.beeSource` now uses `BattleBeeSourceAnalyzer` as the *single* immediate Battle denial estimate. `chooseBeeSource` no longer adds this tactical result a second time. `beeSequence` (activation valuation) and source choice obtain their owner-specific values from the same helper. The persistent Bee denial proxy remains a separate bounded estimate.
- **Optional Plant flips:** `PlantResourceProjection.facingValue` is now available as the common face-up-opportunity estimate. `bestRefresh` and optional flip target choice use the same estimate. Flipping an existing face-up card down receives the negative of that opportunity; declining the optional action has zero incremental benefit. No arbitrary +50 is added to an already-valued facing opportunity.
- **Cultivation Root/Vine replay:** when selecting a spent Plant for `REUSE_SPENT_ROOT_OR_VINE_EFFECT`, compare the same facing-value proxy used by the top-level `bestReuse` branch. In Battle retain the stronger preexisting `BattleEnabledPlantAnalyzer` tactical target evaluator.
- Added a regression test asserting that the activation maximum for optional refresh agrees with selected target.

## Explicit remaining mismatches / research risks

1. Battle replay activation still uses a conservative future-opportunity proxy (`bestReuse`), whereas the Battle target ranks tactical outcomes from `BattleEnabledPlantAnalyzer`. Unifying that would require projecting a complete nested activation without circular recursion in the evaluator.
2. Bee actor-side contribution is common to all sources, but `beeSequence` is a bounded heuristic for future Bee availability. Distinguish it from a physical state transition.
3. `BattleEnabledPlantAnalyzer` still uses `intrinsic + tactical` for many non-specialized effects. The specialized Sapping/collateral branches avoid that combination; other effects warrant per-effect regression checks.
4. Plant preservation, future opportunity, and phase-specific replay priorities use different units. Do not interpret equal priority numbers as equal exact victory-point values.
5. O Edelweiss's nested second-step scoring remains approximate when a nested effect cannot be projected into the evaluation state.

## Verification

Run:

```
./gradlew test --tests '*HumanBaselineEffectStrategyPlantAlignmentTest' --tests '*PlantResourceProjectionTest'
./gradlew test integrationTest simulationTest
```

No game execution code, card CSVs, or fixed AI weights changed.
