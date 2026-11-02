# C04 — Multiplayer mixed-action acceptance matrix (next increment)

This adds a three-player, three-Strike-row matrix using `BattleMainPriority.score` (the real Human Baseline main scoring boundary) rather than individual valuation helper formulas.

It tests a Wisp resource availability reversal, useful versus capped direct die modification while immediate VP stays stable, and First/Final Main Draw stage consistency.

The positive funded-Worm test in `BattleActionCoordinatorTest` already exists in the input archive; its revised unique-row fixture is present. Its success on Ubuntu has **not** been confirmed by this patch. Do not silently weaken it if it fails.

## Important limitations

This matrix scores real candidate families but does not run a whole multiplayer game, does not execute Sunlight as a competing Main candidate (Sunlight is a Support funding another Main), and does not establish a learned or human-validated calibration of the provisional Battle resource coefficients. A03 protection remains unchanged. Complete C04 sign-off requires broader compound and target parity, timed resource choice tests, mixed ranking reversals with explicit human acceptance expectations, and green Kotlin + simulation suites.

Verify:

```bash
./gradlew test --tests '*C04MultiplayerMixedActionMatrixTest' --tests '*BattleActionCoordinatorTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```
