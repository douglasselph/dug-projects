# C04 follow-up: Main-stage Draw parity

This is an incremental correction, NOT a C04 completion certificate.

- `BattleMainPriority.score` now delegates to the existing `BattleFirstMainPriority`, removing an unconditional +10 awarded to Final Main Draw irrespective of current Strike rows.
- This also aligns calls from Sunlight spending and enabling-Support previews with First Main Draw projection.
- `C04MainStageParityTest` checks shared result for First/Final Draw, and no phantom +10 when no die can be drawn.
- No gameplay rules, protected scopes, or learned policies are changed.

Remaining C04 requirements: project direct Plant die changes, multi-target/opponent effects, rerolls, Round effects, Draw without placement capacity and all other ordinary candidate families on a commensurable Battle tactical result scale, with target-selection parity and full regression evidence. Existing fixed 45-70 priorities and the old decision engine still prevent certification.

Ubuntu: `./gradlew test --tests '*C04MainStageParityTest'` and `./gradlew test simulationTest` and `python3 bin/a03_verify_boundaries.py`.
