# Human Baseline Decision Calibration

## Result

No Human Baseline numerical calibration values are changed by this checkpoint.

The targeted certification set supplied enough before-change evidence to test the approved calibration concerns, but did not demonstrate a consistently sensible action losing because of an obviously mis-scaled priority. Changing constants anyway would be optimization without evidence and would violate the purpose of Human Baseline calibration.

## Evidence reviewed

The targeted certification runs classified the following decision families as behaviorally sensible: Mulch, Root Four More, Root Cause, Forget-Me-Not, Queen's Blossom, Saplink Trellis, Bloom Backbone, Bee-loved Bloom, Petal To Die 4, Transplant Tulip, and O Edelweiss.

Specific calibration checks:

- Draw versus Plant activation: observed decisions crossed the boundary in both directions. Plants lost to Draw or other actions when their contextual value was weak and won when immediate effect, Buy threshold, Refresh, or other approved context justified it. No global Draw/Plant scale correction is supported.
- Creature Refresh: immediate Refresh and two-action Refresh changed decisions without acting as hard commands. Refresh sometimes made an otherwise marginal activation worthwhile, while stronger alternatives could still win. No adjustment is supported.
- Phase Preservation: imminent Battle penalties caused Battle-useful Plants to be preserved in ordinary cases, weakened when two-action Refresh was plausible, disappeared on immediate Refresh, and were overcome by sufficiently strong current opportunities. No adjustment is supported.
- Mulch: the observed graded roll curve, Battle-next willingness, high-sided-die preference, weak-upcoming-dice adjustment, Buy-threshold opportunity cost, and reserve effect all produced both accepts and rejects at sensible boundaries. The sparse showing-5 sample is not evidence for retuning.
- Forget-Me-Not: future availability scaled across D6-D20, recycle distance and imminent-Battle acceleration affected choices, and D8/D10 opportunities could be worthwhile without making the effect automatic. The sample was sufficient for behavioral certification but not for precision tuning.
- Petal To Die 4: Gain-D4 valuation included future D4 VP, Trash charged lost D4 VP, and the Battle branch retained graded immediate tactical/VP value. Both Gain and Trash occurred in appropriate contexts. No numerical correction is supported.
- Card-local priorities: the targeted set produced no card whose sensible action consistently lost because of an obviously mis-scaled priority.

## Before / after

There is intentionally no numerical after-state. The calibrated production constants before this checkpoint are retained exactly.

Therefore a controlled-seed before/after rerun would be byte-for-byte the same program with the same inputs and is expected to produce identical decisions and aggregates. No second simulation was run merely to compare an unchanged implementation with itself.

## Tests

No deterministic test is changed because no behavior or calibration contract changed. Existing deterministic tests remain the regression boundary for the certified behavior.

## Gate

Human Baseline numerical calibration is accepted as-is for the next research checkpoint. This is not a claim that the constants are optimal, nor a game-balance judgment. It means the targeted evidence did not justify changing them.
