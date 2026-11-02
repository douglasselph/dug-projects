# C04 — Battle Draw unit migration (increment, not full calibration)

Battle Draw previously reused Cultivation `DrawPriority` (`35 + 4 * expected pip value`),
then added Strike consequence from a different scale. That mixed two unrelated units.

The new Battle-only `BattleDrawUtility` uses `35 + round(expected face)` as an
explicit provisional non-Strike die contribution, plus one existing Strike-row tactical
projection when a legal placement can be projected. It does not add Strike VP again.
Both `BattleMainPriority.intrinsicScore` and `BattleFirstMainPriority` call this same
calculator; no First/Final/Sunlight-specific bonus remains.

## Limitations

* The coefficient of one point per expected face is a stated **calibration
  assumption**, not an empirically fitted Battle tactical exchange rate.
* Without a Battle board view, placement is unprojected; the die contribution
  remains provisional rather than inventing a win or legal capacity.
* Opponent, compound and fallback Plant utilities, Round future resources, and
  realistic mixed-candidate selection have not been fully calibrated.
* Existing tests that expect D4=45 or D20=77 in Battle need careful scenario
  review; do not blindly rewrite all assertions to new numbers.

C04 must remain unchecked until the full mixed-candidate calibration and suite pass.
