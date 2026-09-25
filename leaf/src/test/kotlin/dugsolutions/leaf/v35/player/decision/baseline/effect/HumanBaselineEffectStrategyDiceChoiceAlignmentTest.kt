package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiePairRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDiePairChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HumanBaselineEffectStrategyDiceChoiceAlignmentTest {

    @Test
    fun `optional set-to-three declines when every legal die would get worse`() {
        val context = cultivationContext(
            DieView(index = 0, sides = 6, value = 5),
            DieView(index = 1, sides = 8, value = 7)
        )

        val chosen = HumanBaselineEffectStrategy().chooseOptionalDie(
            ChooseOptionalEffectDieRequest(
                effect = GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3,
                legalChoices = listOf(
                    EffectDieChoice(index = 0, sides = 6, value = 5),
                    EffectDieChoice(index = 1, sides = 8, value = 7)
                ),
                context = context
            )
        )

        assertNull(chosen)
    }

    @Test
    fun `optional set-to-three accepts the die with the best immediate gain`() {
        val context = cultivationContext(
            DieView(index = 0, sides = 6, value = 1),
            DieView(index = 1, sides = 8, value = 2)
        )

        val chosen = HumanBaselineEffectStrategy().chooseOptionalDie(
            ChooseOptionalEffectDieRequest(
                effect = GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3,
                legalChoices = listOf(
                    EffectDieChoice(index = 0, sides = 6, value = 1),
                    EffectDieChoice(index = 1, sides = 8, value = 2)
                ),
                context = context
            )
        )

        assertEquals(0, chosen?.index)
    }

    @Test
    fun `Root Kindred scores the complete source-target pair by realized copied value`() {
        val context = cultivationContext(
            DieView(index = 0, sides = 12, value = 10),
            DieView(index = 1, sides = 6, value = 1),
            DieView(index = 2, sides = 6, value = 5),
            DieView(index = 3, sides = 12, value = 4)
        )
        val strong = EffectDiePairChoice(
            source = EffectDieChoice(index = 0, sides = 12, value = 10),
            target = EffectDieChoice(index = 1, sides = 6, value = 1)
        )
        val weak = EffectDiePairChoice(
            source = EffectDieChoice(index = 2, sides = 6, value = 5),
            target = EffectDieChoice(index = 3, sides = 12, value = 4)
        )

        val chosen = HumanBaselineEffectStrategy().chooseDiePair(
            ChooseEffectDiePairRequest(
                effect = GameEffect.SET_DIE_TO_MATCH_ANOTHER,
                legalChoices = listOf(weak, strong),
                context = context
            )
        )

        assertEquals(strong, chosen)
    }

    @Test
    fun `multi-die choice respects the legal subset bounds while scoring the complete set`() {
        val context = cultivationContext(
            DieView(index = 0, sides = 20, value = 1),
            DieView(index = 1, sides = 6, value = 5),
            DieView(index = 2, sides = 8, value = 2)
        )

        val chosen = HumanBaselineEffectStrategy().chooseDice(
            ChooseEffectDiceRequest(
                effect = GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
                legalChoices = listOf(
                    EffectDieChoice(0, 20, 1),
                    EffectDieChoice(1, 6, 5),
                    EffectDieChoice(2, 8, 2)
                ),
                minChoices = 1,
                maxChoices = 1,
                context = context
            )
        )

        assertEquals(listOf(0), chosen.selected.map { it.index })
    }

    private fun cultivationContext(vararg dice: DieView): DecisionContext =
        DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(hand = dice.toList())
            )
        )
}
