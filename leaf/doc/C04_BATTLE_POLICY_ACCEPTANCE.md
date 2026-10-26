# C04 Battle utility policy and acceptance (increment)

## What changed

The Battle Main scoring paths for immediate Draw, projected Plant effects, fallback Plants and Round resources now reference **one** named provisional origin (`BattleUtilityPolicy.ORIGIN`) instead of four separately hardcoded `35`s. The established non-Strike VP, Wisp option, denial and future-discard access coefficients live alongside it. This is a policy-centralization and regression-control change, **not fitted utility** and not yet full C04 sign-off.

Added real-Main mixed-candidate assertions for projected Plant versus capped Plant, Draw, available/unavailable resources, and immediate VP; they test that ranking evidence responds to board conditions and sources share the policy origin. Existing multicandidate and positive funded-Worm integration tests are part of the acceptance runner.

## Run

`bash doc/C04_RUN_ACCEPTANCE.sh`

This deliberately fails on any failing Kotlin test, simulation test or A03 boundary change. It does **not** automatically approve protected modifications. No mechanics or trained policies are changed.

## Unresolved criteria for C04 sign-off

1. Run the entire Kotlin+simulation suite on Ubuntu, including the corrected positive funded-Worm target test, and resolve actual failures rather than weakening behavior expectations.
2. Establish an *independent*, human-authored and fixed acceptance set of multiple legal candidate rankings, with 3-player/3-row contexts and separate training versus held-out states. Do not treat tests encoding current formulas as human calibration evidence.
3. Fit or justify Wisp, opponent denial, future cycling, Sunlight and recovery-option coefficients against that acceptance set; document sensitivity and uncertainty.
4. Replace remaining Plant fallback adjustment factors and unprojected compound/resource effects; verify downstream target parity and legal action timing for each newly migrated family.
5. Test full real-turn mixed Plant/Draw/Round/Sunlight decisions rather than only entry-point scores, including reversals caused by legal capacity and Support closure.

C04 remains **UNCHECKED** pending those requirements.
