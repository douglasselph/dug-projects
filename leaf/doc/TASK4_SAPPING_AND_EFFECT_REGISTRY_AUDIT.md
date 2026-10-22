# Task 4 — Snapdragon regression and experimental effect registry audit

Source baseline: `leaf(20261022-201541).tgz`. This task adds tests and an audit; it does not change game mechanics or research CSVs.

## Snapdragon behavior confirmed by source inspection

| Effect | Cultivation | Battle target | Final Raise | Minimum dice |
| --- | --- | --- | --- | --- |
| `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW` | Raise any die +2 | Same selected die's row | Original die receives actual sum of opponent reductions | 1 placed own die |
| `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW` | Raise any die +2 | A **different** own placed die's row | The *second (anchor) die* receives actual sum of opponent reductions | 2 placed own dice |

- Both effects reduce every opposing die in the selected row by 2, to minimum face 1. The actual (not nominal) reduction is totalled.
- An empty opposing row yields no extra Raise, but the initial +2 remains.
- Neither effect moves/adds dice; full Strike Squares do not prevent value changes. Opponent-square fullness is irrelevant.
- Initial Raise is capped at selected die's maximum; second Raise is capped at its recipient's maximum.
- The enhanced variant chooses the second target **after** applying the first +2; second target excludes the original die.
- An own die not placed on the grid is not a legal Battle choice.

## Registry / selectability

- Both constants exist in `GameEffect.kt`.
- `DefaultGameEffectExecutor.handlerFor` routes both to `DieValueEffectHandler`.
- `DieValueEffectHandler.canExecute` has a phase-aware branch for each, and `execute` selects the separate implementations.
- `GameEffectDecisionRequirements` maps both to `EFFECT_DIE` (twice in Battle for the enhanced variant, using the same decision mechanism).
- `GameEffectConverter` has distinct explicit text registrations for both. The enhanced code is research-only in both canonical CSV coverage tests.
- Petal P1–P4 and O Edelweiss O1–O2 each have enum, dispatcher, and decision-requirements coverage; both research-only CSV exception lists include their new codes. The legacy Petal and O Edelweiss codes are retained as aliases/defaults.
- The current `data/research/resync/resync-current.csv` and canonical data are unchanged by Task 4.

## Research-readiness warning for Task 5

The **effect executor** correctly differentiates both Snapdragon variants, but Human Baseline handling is not fully symmetric:

- `BattleOwnDieCollateralAnalyzer` and `HumanBaselineEffectStrategy.COLLATERAL_OWN_DIE_BATTLE_TRANSFORMS` specifically project the *same-die* variant only.
- `CardScoringHelpers` recognizes the enhanced variant and assigns a heuristic flexibility bonus (+8), but that is not a full two-stage row/target projection.
- `SappingSnapdragonBaseline` is configured for the original effect only.

Before interpreting expert-vs-human results, Task 5 should audit/improve baseline selection for **both** choices of the enhanced variant without giving it an artificially weak or strong control policy. This is not corrected in Task 4 because Task 5 explicitly owns Human baseline decisions.

## Regression tests

`SappingSnapdragonVariantsRegressionTest.kt` tests:
1. both Cultivation branches (no opponent changes);
2. same-die row targeting and reduction accounting;
3. different-die row targeting and correct credit to anchor;
4. one-die vs. two-die Battle executability;
5. empty opponent rows;
6. full Battle Strike Squares without displacement.

## Validation status

The Gradle wrapper attempts to download Gradle 8.13 and this environment cannot resolve `services.gradle.org`. Tests are **not claimed passing**. Run locally:

```sh
./gradlew test --tests '*SappingSnapdragonVariantsRegressionTest'
./gradlew test integrationTest simulationTest
```
