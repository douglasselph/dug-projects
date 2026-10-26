# C04 — Shared scale for complete opponent/compound Strike projections

This bounded follow-up removes a demonstrable common-scale violation: Sapping and collateral activation scoring previously added a card-specific intrinsic legacy base to a **complete** projected Strike-row effect. Thus the same target-level tactical result could receive very different action rankings depending on the card's arbitrary old base.

`BattleProjectedActionScore` gives these complete opponent/compound projections one common **temporary legacy decision origin (35)** plus the projected tactical adjustment from `LegacyPreferenceBridge`. These action paths use exactly their existing target-level projectors and do not double-count projected Strike VP. Their signatures retain the obsolete `base` argument for caller compatibility; it is intentionally ignored.

This is **not C04 completion**: no common physical tactical utility is yet assigned to all Round, Draw, single-die, reroll, and nested/compound candidate classes. In particular, their action comparisons may still contain old legacy bases. Further work should connect all competitors to a single Battle preference scale and test complete mixed-action selection.

Verify on Ubuntu:

```
./gradlew test --tests '*C04CompleteProjectionScaleTest' --tests '*BattleCollateralActivationPriorityTest' --tests '*BattleSappingSnapdragonProjectorTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```

No A03 rebaseline is needed. This change touches only Human Baseline and its tests.
