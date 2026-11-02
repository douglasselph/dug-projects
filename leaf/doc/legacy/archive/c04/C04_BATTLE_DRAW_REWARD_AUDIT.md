# C04 — Battle Draw reward audit increment

Based on the October 30, 2026 source archive and the 36-scenario human review.

## Change

The existing Battle Draw scorer evaluated expected pips and projected Strike outcomes but ignored Normal Draw roll rewards. The new `BattleDrawRollRewardUtility` adds a probability-weighted **non-Strike** option value for a Critter on face 1 (when Grove has a Bee or Worm) and for a Wisp on face 2 (when the deck has cards and the player is below cap). It never directly adds a Critter to a projected Strike row, so it does not double-count Strike VP.

Current coefficients: Critter option value 2 points; Wisp option uses existing `BattleUtilityPolicy.WISP_OPTION = 5`. Reward chance is `1/sides` for each face. These are provisional calibration coefficients, not human-fitted results.

**Rules discrepancy requiring attention:** `RollResolver.resolveRewardForCurrentValue` currently awards a Wisp for face 2 during Battle Normal Draw, whereas the v36 rule notes describe face-2 Wisp as Cultivation-only. This patch follows current executable code rather than modifying canonical mechanics. Please confirm which rule is intended before final C04 acceptance.

## Tests

```bash
./gradlew test --tests '*C04BattleDrawRewardAuditTest' --tests '*C04MixedCandidateSelectionTest' --tests '*C04BattleDrawScaleTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```

Kotlin compilation and execution **not locally verified**: Gradle 8.13 distribution download is blocked in the current environment. A03 was run independently.

## Follow-up

Check compound and opponent effects, tactical Critter Support reachability/capacity, actual full-action Sunlight continuation, and broader real-game acceptance fixtures. The expected Critter value is intentionally conservative rather than promising immediate Battle +2.
