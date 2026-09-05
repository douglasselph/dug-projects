package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.error.configurationCheck
import dugsolutions.leaf.v35.error.stateCheck
import java.text.Normalizer

/**
 * Converts the printed effect text from the card CSV files into the
 * corresponding game-level [GameEffect].
 *
 * Card identity is intentionally not part of this conversion. If two cards
 * have the same effect text, they resolve to the same [GameEffect].
 *
 * Matching is normalized only for presentation-level differences:
 * whitespace, case, Unicode compatibility forms, smart quotes, and dash
 * variants. It is intentionally not fuzzy. A materially changed or new
 * effect should fail closed to [GameEffect.UNKNOWN] so that the converter
 * must be updated deliberately.
 */
class GameEffectConverter(
    private val onUnknownEffect: (sourceName: String?, effectText: String) -> Unit = { _, _ -> }
) {

    /**
     * @param effectText effect text read from a CSV effect column.
     * @param sourceName optional card/source name used only for diagnostics.
     */
    operator fun invoke(
        effectText: String,
        sourceName: String? = null
    ): GameEffect {
        val effect = effectsByText[normalize(effectText)]
        if (effect != null) return effect

        onUnknownEffect(sourceName, effectText)
        return GameEffect.UNKNOWN
    }

    companion object {
        private val whitespace = Regex("""\s+""")

        private fun normalize(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace(whitespace, " ")
                .trim()
                .lowercase()

        private val effectsByText: Map<String, GameEffect> = buildMap {
            register(
                """
🎲 Raise ×2 one die
                """,
                GameEffect.DOUBLE_ONE_DIE
            )

            register(
                """
🎲 Raise a die +4
                """,
                GameEffect.RAISE_DIE_PLUS_4
            )

            register(
                """
Reroll a die until it rolls 3+.
Ignore Roll Rewards.
                """,
                GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS
            )

            register(
                """
Gain 1 Water.
<battle/> Spend 1 Water to reroll 2 of your dice, or 1 of your opponent's.
                """,
                GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE
            )

            register(
                """
🎲 Raise any die +1.
<battle/> Remove all your dice 
from one Strike Square,
you no longer partipate in that strike.
                """,
                GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE
            )

            register(
                """
Gain 1 worm, 
Each worm <worm/> is worth 2 more this round.
                """,
                GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND
            )

            register(
                """
Set any die showing 2+ to 1;
gain 1 VP per 1 showing.
No Roll Rewards earned.
                """,
                GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE
            )

            register(
                """
<cultivation/> Cultivation Only: Discard 
any number of dice; draw that many.

<battle/> Battle Only: Reroll 1 die.
                """,
                GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE
            )

            register(
                """
Upgrade a die; 
use it now.
                """,
                GameEffect.UPGRADE_DIE_AND_USE_NOW
            )

            register(
                """
Flip one of your dice
to its opposite face
                """,
                GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE
            )

            register(
                """
Set a die to match
the value of another
                """,
                GameEffect.SET_DIE_TO_MATCH_ANOTHER
            )

            register(
                """
Mulch any die
from Dice Discard Bin.
                """,
                GameEffect.MULCH_DIE_FROM_DISCARD
            )

            register(
                """
🎲 Raise any die +2.

<battle/> In it's Strike Row, 
reduce all opposing dice by 2;
Raise it more by the total reduced.
                """,
                GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW
            )

            register(
                """
Steal 1 Butterfly.
Turn all your Butterflies face up.
                """,
                GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES
            )

            register(
                """
Choose 1:
Gain 1 D4 to Hand→ set it to 4, or,
Trash 1 D4 → 🎲 Raise all your dice +4
                """,
                GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4
            )

            register(
                """
Discard 1 die; 
draw 1 to replace it.
<battle/> You may swap 
two of your dice.
                """,
                GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE
            )

            register(
                """
Gain or Steal a Bee <bee/>.
Each of your bees is worth 4 this round.
                """,
                GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
            )

            register(
                """
For each grafted
Vine or Flower, 
🎲 Raise any die +1
                """,
                GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER
            )

            register(
                """
🎲 Raise a die +1.

<battle/> Flip each opposing die higher than it in its Strike Row.
                """,
                GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
            )

            register(
                """
Reroll one of your die.

<battle/> In one strike row, 
all opponent dice higher than one of your dice must reroll
                """,
                GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
            )

            register(
                """
🎲 Raise any die +1
then for each die showing its maximum value,
draw 1 die
                """,
                GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE
            )

            register(
                """
Choose any 1 die from your Discard. 
Roll it into your Hand.
                """,
                GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND
            )

            register(
                """
Twice you may play another card 
or flip another card
                """,
                GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
            )

            register(
                """
Draw 2 dice
                """,
                GameEffect.DRAW_TWO_DICE
            )

            register(
                """
🎲 Raise any die +1.
                """,
                GameEffect.RAISE_ANY_DIE_PLUS_1
            )

            register(
                """
Wound 1 card of your choice of an opponent's.
You must choose a face up card first if there is one.
                """,
                GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE
            )

            register(
                """
Use the effect of another spent Root or Vine
                """,
                GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
            )

            register(
                """
Set a die showing your lowest value
to its maximum
                """,
                GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX
            )

            register(
                """
<cultivation/> May flip 1 of your Plant cards.
<battle/> Each opponent suffers one wound.
                """,
                GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE
            )

            register(
                """
🎲 Raise all your dice +2
                """,
                GameEffect.RAISE_ALL_DICE_PLUS_2
            )

            register(
                """
Trash a worm <worm/> or bee <bee/>
to 🎲 Raise any die by 5.
                """,
                GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5
            )

            register(
                """
Discard 1 die; draw 2.

<battle/> Place 1 drawn die in the discarded die's Strike Square.
                """,
                GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE
            )

            register(
                """
For each Root or Vine,
🎲 Raise any die +1
                """,
                GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE
            )

            register(
                """
<cultivation/> Cultivation Only: 
You may set any die to 3.
<battle/> Choose 1 Strike row. Reduce all opponent dice there by 3.
                """,
                GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3
            )

            register(
                """
Set a die up to a D12 
to its maximum value
                """,
                GameEffect.SET_DIE_UP_TO_D12_TO_MAX
            )

            register(
                """
Gain 1 VP
                """,
                GameEffect.GAIN_ONE_VP
            )

            register(
                """
Gain any 2 critters
(🪱/🐝)
                """,
                GameEffect.GAIN_ANY_TWO_CRITTERS
            )

            register(
                """
Gain or Refresh
Green Butterfly
                """,
                GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY
            )

            register(
                """
Gain or Refresh 
Purple Butterfly
                """,
                GameEffect.GAIN_OR_REFRESH_PURPLE_BUTTERFLY
            )

            register(
                """
Gain or Refresh 
Red Butterfly
                """,
                GameEffect.GAIN_OR_REFRESH_RED_BUTTERFLY
            )

            register(
                """
Gain or Refresh 
Yellow Butterfly
                """,
                GameEffect.GAIN_OR_REFRESH_YELLOW_BUTTERFLY
            )

            register(
                """
Gain Mulch;
Store 1 die from your discard on it.
                """,
                GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD
            )

            register(
                """
(Play immediately)
All players must reroll all their dice.
You may keep one of yours.
                """,
                GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN
            )

            register(
                """
Each opponent keeps up to 3 Wisps; 
You keep 4. 
Trash the rest.
                """,
                GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS
            )

            register(
                """
<battle/> Swap one of your dice
with an opponent’s 
same-size die
(no re-rolls)
                """,
                GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE
            )

            register(
                """
Upgrade +2 steps — 
skip missing sizes; 
use gained die now
                """,
                GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW
            )

            register(
                """
<battle/> Battle:
Resolve a Strike immediately.
Then return all dice & critters
from that row.
                """,
                GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
            )

            register(
                """
Gain 1 Wisp card
                """,
                GameEffect.GAIN_ONE_WISP
            )

            register(
                """
Gain D10.
Place in Discard.
                """,
                GameEffect.GAIN_D10_TO_DISCARD
            )

            register(
                """
Steal 1 Wisp card randomly from any opponent
                """,
                GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT
            )

            register(
                """
Gain any die.
Place in Discard.
                """,
                GameEffect.GAIN_ANY_DIE_TO_DISCARD
            )

            register(
                """
Gain 2 Worms
                """,
                GameEffect.GAIN_TWO_WORMS
            )

            register(
                """
Gain D20. 
Place in Discard.
                """,
                GameEffect.GAIN_D20_TO_DISCARD
            )

            register(
                """
Gain D12. 
Place in Discard.
                """,
                GameEffect.GAIN_D12_TO_DISCARD
            )

            register(
                """
Steal 1 Wisp card randomly from all opponents
                """,
                GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS
            )

            register(
                """
Upgrade a die
from your hand
                """,
                GameEffect.UPGRADE_DIE_FROM_HAND
            )

            register(
                """
Mulch a die
from your hand
                """,
                GameEffect.MULCH_DIE_FROM_HAND
            )

            register(
                """
🎲 Raise a die +3
                """,
                GameEffect.RAISE_DIE_PLUS_3
            )

            register(
                """
Gain Water token
                """,
                GameEffect.GAIN_WATER_TOKEN
            )
        }

        private fun MutableMap<String, GameEffect>.register(
            effectText: String,
            effect: GameEffect
        ) {
            val key = normalize(effectText)
            val previous = put(key, effect)

            configurationCheck(previous == null || previous == effect) {
                "Two GameEffects normalize to the same effect text: $previous and $effect"
            }
        }
    }
}
