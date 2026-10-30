# Task 5B.6 — Sapping Snapdragon Battle projection

Both effect variants use `BattleSappingSnapdragonProjector`. It applies the +2 Raise with the die maximum, drains each eligible opponent die in the chosen anchor row with floor 1, and credits the actual amount removed to the anchor die with its maximum. It projects all changed rows without altering the live Battle snapshot.

- **S1**: the initial die is also the anchor.
- **S2**: the initial die and anchor are distinct; may affect two rows.
- Each candidate is evaluated via `BattleRowAssessor`, `BattleActionAnalyzer`, `BattleSwingEvaluator`, and `BattleVpImpact`.
- At top-level activation S2 considers every ordered pair of distinct located dice. At the first die decision it considers all possible anchors; when selecting an anchor after the first Raise is already applied by the engine, `initialRaisePending=false` avoids applying +2 again.
- The production handlers and CSV configurations remain unchanged.

## Verification

`./gradlew test --tests '*BattleSappingSnapdragonProjectorTest'`

Compilation/tests have **not** been verified here: the offline Gradle invocation timed out.

## Remaining check

Task 5B.7 should review any intrinsic card-score adjustments that also reward the initial Raise or row drain, to avoid double counting projected Battle consequences.
