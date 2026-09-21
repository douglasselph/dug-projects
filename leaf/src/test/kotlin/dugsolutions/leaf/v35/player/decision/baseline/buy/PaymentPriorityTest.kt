package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.player.decision.buy.BuyCritterResource
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PaymentPriorityTest {
    @Test
    fun `at the normal reserve spending a Bee is less costly than spending a Worm`() {
        val context = DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    bees = HumanBaselineBuyStrategy.TARGET_BEE_RESERVE,
                    worms = HumanBaselineBuyStrategy.TARGET_WORM_RESERVE
                )
            )
        )
        val beePayment = BuyPayment(
            critters = listOf(BuyCritterResource(Critter.BEE, Critter.BEE.baseValue))
        )
        val wormPayment = BuyPayment(
            critters = listOf(BuyCritterResource(Critter.WORM, Critter.WORM.baseValue))
        )

        val beeScore = PaymentPriority.score(context, beePayment, cost = 1)
        val wormScore = PaymentPriority.score(context, wormPayment, cost = 1)

        val beeReservePenalty = beeScore.adjustments.single { it.reason == "Preserve Bee reserve" }.amount
        val wormReservePenalty = wormScore.adjustments.single { it.reason == "Preserve Worm reserve" }.amount

        assertEquals(-20, beeReservePenalty)
        assertEquals(-25, wormReservePenalty)
    }
}
