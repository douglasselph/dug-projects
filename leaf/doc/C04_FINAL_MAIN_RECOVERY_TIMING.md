# C04 — Final Main recovery timing (bounded integration)

The real `HumanBaselineBattleStrategy.chooseTurnAction` selects from the engine's
legal `BattleTurnAction.FinalMain` requests. A final Main ends the current Battle
Support window: an open Strike row is not evidence that Worms gained by that Main
can be placed afterward. Likewise a Plant refresh cannot produce another normal
Main this Battle. `BattleMainPriority.finalMainScore` now applies that timing for
`GAIN_TWO_WORMS` and `REFRESH_CREATURE`, when no future Battle opportunity exists.

Other Main actions retain the existing scoring paths, and first Main recovery
still uses conservative visible opportunities. Future-Battle resource valuation
is explicitly provisional. This does **not** certify a complete legal Support
projection, Sunlight-funded Main sequencing, or C04 final calibration.

Verify with `./gradlew test --tests '*C04FinalMainRecoveryTimingTest'`, then
`./gradlew test simulationTest` and `python3 bin/a03_verify_boundaries.py`.
