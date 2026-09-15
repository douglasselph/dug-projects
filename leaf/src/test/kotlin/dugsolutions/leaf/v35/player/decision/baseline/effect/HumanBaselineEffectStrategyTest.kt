package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiceRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategyTest {
    @Test
    fun `Root Cause prefers a D20 showing one over a mediocre D6`() {
        val context = DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    hand = listOf(
                        DieView(index = 0, sides = 6, value = 3),
                        DieView(index = 1, sides = 20, value = 1)
                    )
                )
            )
        )
        val strategy = HumanBaselineEffectStrategy()
        val chosen = strategy.chooseDie(
            ChooseEffectDieRequest(
                effect = GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE,
                legalChoices = listOf(
                    EffectDieChoice(index = 0, sides = 6, value = 3),
                    EffectDieChoice(index = 1, sides = 20, value = 1)
                ),
                context = context
            )
        )
        assertEquals(1, chosen.index)
    }

    @Test
    fun `Root Recall scores complete legal subsets instead of greedily keeping a fixed count`() {
        val context = DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    hand = listOf(
                        DieView(index = 0, sides = 20, value = 1),
                        DieView(index = 1, sides = 6, value = 5),
                        DieView(index = 2, sides = 8, value = 2)
                    )
                )
            )
        )
        val strategy = HumanBaselineEffectStrategy()
        val chosen = strategy.chooseDice(
            ChooseEffectDiceRequest(
                effect = GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
                legalChoices = listOf(
                    EffectDieChoice(0, 20, 1),
                    EffectDieChoice(1, 6, 5),
                    EffectDieChoice(2, 8, 2)
                ),
                minChoices = 0,
                maxChoices = 3,
                context = context
            )
        )

        assertEquals(setOf(0, 2), chosen.selected.map { it.index }.toSet())
    }
}
