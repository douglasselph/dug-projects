# C04 review expansion — legality and target boundary

Source: leaf(20261030-154741).tgz and author's V3 36-case answers.

## Changes
- The original four mixed-action score comparisons remain diagnostics.
- Added exact reviewed target projections V3-01, V3-05, V3-31. These compare a forced one-die Raise with Draw, separately from an unrestricted Plant activation that can choose another die.
- Added board-based observations for V3-04, V3-33 and V3-34. V3-04 is explicitly non-comparable until the exact +2 Plant exists in the fixture.
- Replaced colliding per-row hand indices with unique IDs across players, rows and squares.
- Validates the face range and die size of every placed die in the constructed views.

## Critical distinction
These are populated DecisionContext *snapshots*, NOT complete engine-backed Game/BattleState fixtures. The actual coordinator `BattleActionCoordinator.mainActions` additionally checks die draw-and-place capacity, ready Plants and `effectExecutor.canExecute`, and whether a Round effect can execute; card target legality is decided again during effect execution. None of these new cases is certified as engine legal by the snapshot checks. A subsequent engine-backed fixture remains necessary for a bona fide acceptance result.

## Not claims of success
- Assertions do not force human agreement; logs report the relative scorers. No production scoring coefficients changed.
- Kotlin/Gradle execution was attempted with a portable offline cache, but did not complete within the available runtime.
- The exact forced-target score is based on the shared tactical projector and is not an executed target selection.

## Inspect output
`./gradlew test --tests '*C04ReviewedPreferenceAcceptanceTest' --info`

Look for C04_REVIEW, C04_FORCED_TARGET, C04_EXPLORATORY records. For rejected judgments, retain the author's original answer; investigate eligibility, card text, round stage and support window before tuning.

## Next executable milestone
Construct real Game/BattleState scenarios and capture coordinator-generated legal Main actions plus actual effect die choices; assert each reviewed candidate and chosen die exist in these lists before declaring a case accepted. Prefer a small diverse certified sample over more authored candidate specs.
