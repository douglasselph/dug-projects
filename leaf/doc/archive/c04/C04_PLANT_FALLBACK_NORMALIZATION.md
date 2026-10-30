# C04 Plant fallback normalization — bounded follow-up

This change removes card-specific legacy **base priorities** from Battle Plant
Main candidates that do not have a complete Strike-row projection. The
production entry point `BattleMainPriority.intrinsicScore` now uses
`BattlePlantFallbackScore.normalize` for fallback paths, retaining their
existing contextual adjustments and research no-op disqualification.

This is **not a completed C04 tactical calibration**. Some contextual
adjustments still use legacy point factors, and the Draw and Round scorers
still have different provisional conventions. An unsupported compound effect
must not be described as fully projected merely because its base is 35.

## Verification

```
./gradlew test --tests '*C04PlantFallbackScoreTest' \
  --tests '*C04MixedCandidateSelectionTest' \
  --tests '*HumanBaselineBattleStrategyTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```

The full C04 check-off criterion remains actual mixed Draw/Plant/Round
selection calibrated to the same Battle consequences, with tests including
compound rerolls, opponent outcomes, and capacity limits.
