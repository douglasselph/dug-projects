# C04 — Contextual Worm/Refresh opportunities and state reversals

This increment replaces unconditional 35-point Round fallbacks for `GAIN_TWO_WORMS` and `REFRESH_CREATURE` with bounded observable opportunities. It does not change gameplay mechanics.

* Worm acquisition: requires shared Worm supply and an observable open, non-withdrawn Strike-row destination or a face-down Plant. It accounts for existing Worm stock. With no observable destination or no stock it returns zero, rather than a fabricated tactical benefit.
* Refresh: requires a face-down Plant and an active Battle view for a player not marked done. It assigns a **minimal option estimate of one point**, not an invented Plant tactical activation or Strike VP. Its legal future activation remains unverified in this decision snapshot.
* Tests use three Strike rows, two player IDs, resource exhaustion, closed rows and completed turn states. They compare scored effects under changed state and demonstrate selection reversals against an unavailable Wisp.

Important limitations: the main-action scorer does not receive the full available legal-action list or exact remaining Main/Support stage, so it cannot prove a subsequent Worm use or refreshed-Plant activation will be legal. Full C04 completion requires stage-aware action opportunity projection, calibrated non-Strike resource exchange rates, and multiplayer mixed Plant/Draw/Round/Sunlight selection acceptance cases. The new tests are not a statistical calibration of utility coefficients.

Suggested Ubuntu verification:

```
./gradlew test --tests '*C04RecoveryOpportunityMatrixTest' --tests '*C04ResourceUtilityParityTest' --tests '*C04RoundScaleMixedSelectionTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```
