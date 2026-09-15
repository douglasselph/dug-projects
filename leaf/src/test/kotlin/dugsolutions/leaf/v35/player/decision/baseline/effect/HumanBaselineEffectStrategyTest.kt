package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
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
}
