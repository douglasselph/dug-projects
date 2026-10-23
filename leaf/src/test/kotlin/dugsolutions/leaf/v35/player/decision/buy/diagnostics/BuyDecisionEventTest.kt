package dugsolutions.leaf.v35.player.decision.buy.diagnostics

import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BuyDecisionEventTest {
    private val d12 = BuyItem.Die(DieSides.D12)
    private val d20 = BuyItem.Die(DieSides.D20)

    @Test fun `captures identical request choices and distinguishes legal from market options`() {
        val request = ChoosePurchaseRequest(
            options = listOf(d12), marketOptions = listOf(d12, d20), purchasesMadeThisBuy = 2
        )
        val event = BuyDecisionEvent.capture(7, request, BuyChoice.Purchase(d12), BuyChoice.Done)
        assertEquals(7, event.sequenceInObserver)
        assertEquals(2, event.purchasesMadeThisBuy)
        assertEquals(BuyDisagreementKind.BASELINE_BUYS_EXPERT_DONE, event.kind)
        assertTrue(event.disagrees)
        assertEquals(listOf(BuySelection.Die(12)), event.legalOptions.map { it.selection })
        assertEquals(listOf(BuySelection.Die(12), BuySelection.Die(20)), event.marketOptions.map { it.selection })
        assertEquals(0, event.diceOnlyPurchasingPower)
        assertEquals(0, event.aggregatePurchasingPower)
    }

    @Test fun `wrapper emits event without changing executed choice`() {
        val events = mutableListOf<BuyDecisionEvent>()
        val actual = object : BuyStrategy {
            override fun choosePurchase(request: ChoosePurchaseRequest) = BuyChoice.Purchase(d20)
            override fun choosePayment(request: ChoosePaymentRequest) = BuyPayment()
        }
        val baseline = object : BuyStrategy {
            override fun choosePurchase(request: ChoosePurchaseRequest) = BuyChoice.Purchase(d12)
            override fun choosePayment(request: ChoosePaymentRequest) = BuyPayment()
        }
        val wrapper = ShadowBuyStrategy(actual, { baseline }, {}, events::add)
        assertEquals(BuyChoice.Purchase(d20), wrapper.choosePurchase(ChoosePurchaseRequest(listOf(d12, d20))))
        assertEquals(1, events.size)
        assertEquals(BuyDisagreementKind.DIFFERENT_ITEM, events.single().kind)
        assertEquals(BuySelection.Die(12), events.single().baseline)
        assertEquals(BuySelection.Die(20), events.single().expert)
    }
}
