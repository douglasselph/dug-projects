# C04 Round-effect context follow-up (increment only)

This increment routes Round Main effect valuations through BattleRoundEffectPriority.
It uses observable Wisp deck availability, own Wisp capacity, opponent Wisp counts,
and remaining rounds for dice delivered into discard. It does not pretend these
resource effects produce immediate Strike-row consequence. Their interim
legacy-point valuations remain provisional; **C04 is not finished**.

Remaining: common phase tactical unit, comprehensive compound effects, opponent
die target projectors, unified Plant/Round/Draw ranking, and execution parity.
The current semantics of stealing Wisps at capacity and future discard draw
opportunities should be checked against the game executor before final calibration.

Verification: `./gradlew test --tests '*C04RoundEffectContextTest'` and
`./gradlew test simulationTest`; `python3 bin/a03_verify_boundaries.py`.
