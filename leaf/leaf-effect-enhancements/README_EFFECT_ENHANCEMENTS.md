# Effect Enhancements Patch

This patch adds research-only source support for the currently proposed Plant effect variants. It does not change `resync-current.csv` or make any of these variants canonical.

## New effect IDs

- `GAIN_VP_PER_ONE_SHOWING`
  - Root Down Payment simplification: gain 1 VP per die in Hand showing 1.
- `RAISE_ALL_D8S_PLUS_1`
  - Berry Tasty already existed and remains supported: raise all D8s +1.
- `GAIN_OR_STEAL_BEE_AND_BOOST_BEES_PLUS_2_THIS_ROUND`
  - Bee-loved Bloom candidate: gain/steal a Bee and add +2 to Bee value for the round; repeated resolutions stack.
- `GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW`
  - Petal To Die 4 candidate: gain/steal a D4, then discard any number of D4s from Hand and Draw the same number.
- `REROLL_DIE_ON_3_DRAW_ONE_CULTIVATION_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3`
  - Vine and Punishment candidate A: Cultivation reroll/on-3 Draw; Battle keeps row reduction by 3.
- `REROLL_DIE_ON_3_DRAW_ONE_AND_REDUCE_OPPOSING_STRIKE_ROW_BY_3`
  - Vine and Punishment candidate B: reroll/on-3 Draw in both phases; Battle also keeps row reduction by 3.
- `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW`
  - Enhanced Sapping Snapdragon candidate: Raise one die +2; in Battle choose another die as the row anchor, reduce opposing dice there by 2, then raise the anchor by the total reduced.

## Petal To Die 4 research semantics

“Steal a D4” is implemented broadly for research: the actor can select an exact opponent-owned D4 from Supply, Hand, or Discard, or gain a D4 from the Grove. The acquired D4 goes to the actor's Dice Discard. This can be narrowed later if the final wording should restrict which zone can be stolen from.

Discarded D4s are moved from Hand to Dice Discard, not trashed, so they remain owned for end-game D4 scoring.

## Vine and Punishment research semantics

Rerolls and Draws use normal Roll Rewards. In Battle, a drawn die is placed with normal Battle effect placement; if no legal placement exists, it goes to Dice Discard.

## Verification

Run against the supplied source tree with the portable Gradle environment:

- `compileKotlin` — passed
- `compileTestKotlin` — passed
- `test` — passed
- `integrationTest simulationTest` — passed

The new effect IDs are explicitly treated as research-only by the card-effect contract test until a CSV/resync chooses them.
