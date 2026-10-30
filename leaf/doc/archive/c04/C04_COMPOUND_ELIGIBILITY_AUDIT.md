# C04 compound-effect audit: first increment

Source: `leaf(20261030-150754).tgz`.

## Roll Rewards verified from executable code

`RollResolver.resolveRewardForCurrentValue`: face 1 -> available Bee/Worm;
face 2 -> Wisp; other faces -> no reward. `BattleRound` and
`BattleActionCoordinator` call that same resolver. There is no Cultivation-only
guard. Individual effects may explicitly specify `RollRewardPolicy.IGNORE`.

## Compound effects

- Bloom Backflip: the evaluator raises the selected own die, then flips *all*
  qualifying opposing dice in its row, matching the executor.
- Sapping Snapdragon: a separate deterministic complete-effect projector
  already feeds both Main activation and target selection.
- Gust of Petals: the evaluator estimates the actor's first reroll and the
  opposing dice above the lowest own die on a subsequently chosen row. The
  executor rerolls the actor's die first, then chooses a row and resolves all
  eligible opponent rerolls. The current evaluator uses the **expected actor
  face** to project later eligibility, not exact enumeration of the actor's
  possible face outcomes. That approximation is a remaining C04 calibration
  issue, not evidence of incorrect game execution.

Added three-player, three-Strike-row tests for non-eligible Gust opponents,
Bloom multi-opponent projection, and activation/target parity. These tests do
not claim to establish calibrated human preferences.

Run:

```bash
./gradlew test --tests '*C04CompoundEligibilityAuditTest' \
  --tests '*C04CollateralTargetProjectionParityTest' \
  --tests '*C04CollateralEndToEndTargetChoiceTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```
