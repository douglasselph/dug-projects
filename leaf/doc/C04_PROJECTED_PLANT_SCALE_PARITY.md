# C04 projected Plant scale parity follow-up

This increment removes card-specific legacy base priorities from **complete**
Battle Strike-row projections for deterministic direct die modifications and
pure reroll-until-3+ actions. Both use `BattleProjectedActionScore`, as do
Sapping and collateral effects. A complete Strike-row projection must not
inherit unrelated 50–100 point Plant priorities.

The real Battle Main entry point already invokes these effect-specific scorers,
so this changes actual candidate rankings. It does not change game mechanics,
card CSVs, policy weights, or protected rules.

The new `C04ProjectedPlantScaleParityTest` exercises representative legal Battle
rows and checks that drastically different legacy bases produce equal action
preferences and match the common projection scorer.

**C04 is NOT complete.** Other Plant effects still fall back to intrinsic
card-specific priorities, and Round effects still have provisional contextual
legacy scores. A complete C04 certification requires effect inventory, common
valuation for non-tactical effects, multi-effect and target integration, and
mixed actual selection tests. Do not interpret this narrow parity fix as fully
commensurable Draw/Plant/Round ranking.

Verify:

```
./gradlew test --tests '*C04ProjectedPlantScaleParityTest' \
  --tests '*C04DirectDieActivationPriorityTest' \
  --tests '*C04RerollActivationPriorityTest' \
  --tests '*C04MixedCandidateSelectionTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```
