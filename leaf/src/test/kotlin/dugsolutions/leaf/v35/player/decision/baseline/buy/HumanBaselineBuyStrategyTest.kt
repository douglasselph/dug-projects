package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HumanBaselineBuyStrategyTest {
    @Test
    fun `purchase candidates compete with Done`() {
        val chosen = HumanBaselineBuyStrategy().choosePurchase(
            ChoosePurchaseRequest(
                options = listOf(BuyItem.Die(DieSides.D4)),
                context = DecisionContext.EMPTY.copy(phase = RoundCardType.CULTIVATION)
            )
        )
        val purchase = assertIs<BuyChoice.Purchase>(chosen)
        assertEquals(BuyItem.Die(DieSides.D4), purchase.item)
    }

    @Test
    fun `no offered purchases returns Done`() {
        assertEquals(
            BuyChoice.Done,
            HumanBaselineBuyStrategy().choosePurchase(
                ChoosePurchaseRequest(
                    options = emptyList(),
                    context = DecisionContext.EMPTY.copy(phase = RoundCardType.CULTIVATION)
                )
            )
        )
    }

    @Test
    fun `complete payment candidates prefer exact sufficient payment`() {
        val chosen = HumanBaselineBuyStrategy().choosePayment(
            ChoosePaymentRequest(
                item = BuyItem.Die(DieSides.D6),
                availableDice = listOf(
                    BuyDieResource(sides = 4, value = 4),
                    BuyDieResource(sides = 6, value = 6)
                ),
                availableCritters = emptyList(),
                context = DecisionContext.EMPTY.copy(phase = RoundCardType.CULTIVATION)
            )
        )

        assertEquals(listOf(BuyDieResource(6, 6)), chosen.dice)
    }
}
