# C04 — Multiplayer row-target parity increment

Source: leaf(20261026-174112).tgz.

`SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3` previously used
`BattleCollateralActivationPriority.targetProjections` for its activation
preference, but recomputed per-row values independently in
`HumanBaselineEffectStrategy.chooseStrikeRow`.

The shared `TargetProjection` now optionally records its Strike row, and the
actual row selector consumes these projections, filtering to engine-supplied
legal rows. No mechanics, resolved effects, card CSVs or frozen policies change.

`C04MultiplayerRowTargetParityTest` constructs a three-player, three-row Battle
state, checks that the real downstream row choice maximizes the same rounded
projected utility used at activation, and checks a restricted legal-row case.

## Verification

`python3 bin/a03_verify_boundaries.py`: PASS, 480/480 protected files unchanged.
`python3 -m unittest discover -s bin/tests -p 'test_*.py' -q`: 52 tests PASS.
Kotlin compilation and tests: NOT VERIFIED in this environment.

On Ubuntu:

```
./gradlew test --tests '*C04MultiplayerRowTargetParityTest' --tests '*C04CollateralEndToEndTargetChoiceTest' --tests '*C04CollateralTargetProjectionParityTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```

## Still outside this increment

Compound effects other than the previously migrated Bloom Backflip and Gust
of Petals and this row-target effect; especially multi-stage draw/discard/
refresh combinations. Tactical valuation of Worms/refresh and other contextual
resources. Empirical calibration of all Battle utility coefficients and shared
origin. Broader acceptance-matrix fixtures with actual Plant/Draw/Round/
Sunlight action execution and changed-state ranking reversals. Full Kotlin
regression confirmation before C04 sign-off.
