# Task 5B.9A — Battle collateral scoring alignment

Source: leaf(20261022-220317).tgz.

## Implemented

The new `BattleCollateralActivationPriority` uses the existing target-level Battle projection machinery to value the best legal immediate realization when deciding whether to activate three Plants:

- **Bloom Backflip**: enumerates actor dice and projects its capped +1 followed by flips of qualifying opposing dice in the same row via `BattleOwnDieCollateralAnalyzer`.
- **Gust of Petals**: enumerates actor reroll targets and evaluates the best visible expected follow-up row using `BattleGustOfPetalsTargetAnalyzer`. Unknown rerolls use mean face value; no mechanical RNG is consumed.
- **Vine and Punishment**: enumerates open Strike Rows and projects each opponent die's capped -3 using `BattleDeterministicStrikeRowTargetAnalyzer`.

The same projection is now used to value a top-level Battle Main activation (`BattleMainPriority`) and a Plant made available by Refresh (`BattleEnabledPlantAnalyzer`). The score is `intrinsic.base + best projected tactical swing`, rather than adding the pre-existing card-specific flat bonus to the projection. Effect execution, legality, CSVs and research weights are unchanged.

## Already implemented before this task

Sapping Snapdragon S1/S2 has its own shared projector; its activation and target selection are already aligned. Transplant Tulip's swap-pair target has `BattleOwnDieSwapPairAnalyzer`. Gust and Vine's chosen row and Bloom's target already had detailed analyses; this task reuses rather than duplicates them.

## Remaining work / caveats

1. The **Vine and Punishment** effect includes a Battle die-to-3 branch as well as the opponent row reduction. This patch aligns the row-reduction branch, but does not optimize the alternative branch at top-level activation. Full branch comparison remains an outstanding piece of 5B.9A.
2. **Transplant Tulip** still requires a complete top-level Draw + swap + Raise prediction; the existing target-level pair projection is not the whole effect.
3. **Gust of Petals** is an expected-value projection: after the actor reroll, the actual available row target may change. The live decision already re-evaluates that target.
4. Additional effect families (Wisp collateral, nested actions, opponent flips) remain future work; this is not blanket certification of every Battle effect.
5. A static best immediate Battle swing is not the same as fully modeling the long-term utility of all Plants; downstream policy calibration remains important.

## Tests

`BattleCollateralActivationPriorityTest` compares the activation tactical value with the maximum legal target-level tactical analysis for each integrated effect, and checks null fallbacks. Run:

`./gradlew test --tests '*BattleCollateralActivationPriorityTest'`

Then `./gradlew test integrationTest simulationTest`.
