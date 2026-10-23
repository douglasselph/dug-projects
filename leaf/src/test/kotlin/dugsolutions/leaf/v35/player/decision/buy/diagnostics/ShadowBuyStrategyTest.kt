package dugsolutions.leaf.v35.player.decision.buy.diagnostics

import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShadowBuyStrategyTest {
    private val d12 = BuyItem.Die(DieSides.D12)
    private val d20 = BuyItem.Die(DieSides.D20)
    private val payment = BuyPayment(listOf(BuyDieResource(20, 12)))

    private class Stub(
        val choose: (ChoosePurchaseRequest) -> BuyChoice,
        val pay: (ChoosePaymentRequest) -> BuyPayment = { BuyPayment() }
    ) : BuyStrategy {
        var purchaseCalls = 0
        var paymentCalls = 0
        override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
            purchaseCalls++
            return choose(request)
        }
        override fun choosePayment(request: ChoosePaymentRequest): BuyPayment {
            paymentCalls++
            return pay(request)
        }
    }

    @Test fun `agreement retains shadow only when payment also agrees`() {
        val actual = Stub({ BuyChoice.Purchase(d12) }, { payment })
        val shadows = mutableListOf<Stub>()
        val comparisons = mutableListOf<BuyRecommendationComparison>()
        val observer = ShadowBuyStrategy(actual, {
            Stub({ BuyChoice.Purchase(d12) }, { payment }).also(shadows::add)
        }, comparisons::add)
        assertEquals(BuyChoice.Purchase(d12), observer.choosePurchase(ChoosePurchaseRequest(listOf(d12), purchasesMadeThisBuy = 0)))
        assertSame(payment, observer.choosePayment(ChoosePaymentRequest(d12, listOf(BuyDieResource(20, 12)), emptyList())))
        observer.choosePurchase(ChoosePurchaseRequest(listOf(d12), purchasesMadeThisBuy = 1))
        assertEquals(1, shadows.size)
        assertEquals(2, shadows.single().purchaseCalls)
        assertEquals(1, shadows.single().paymentCalls)
        assertEquals(2, comparisons.size)
        assertFalse(comparisons.first().disagrees)
    }

    @Test fun `item divergence discards hypothetical continuation`() {
        val actual = Stub({ BuyChoice.Purchase(d20) })
        val shadows = mutableListOf<Stub>()
        val comparisons = mutableListOf<BuyRecommendationComparison>()
        val observer = ShadowBuyStrategy(actual, {
            Stub({ BuyChoice.Purchase(d12) }).also(shadows::add)
        }, comparisons::add)
        observer.choosePurchase(ChoosePurchaseRequest(listOf(d12, d20), purchasesMadeThisBuy = 0))
        observer.choosePayment(ChoosePaymentRequest(d20, listOf(BuyDieResource(20, 20)), emptyList()))
        observer.choosePurchase(ChoosePurchaseRequest(listOf(d12, d20), purchasesMadeThisBuy = 1))
        assertEquals(2, shadows.size)
        assertEquals(BuyDisagreementKind.DIFFERENT_ITEM, comparisons.first().kind)
    }

    @Test fun `different payment invalidates shadow cached plan`() {
        val actual = Stub({ BuyChoice.Purchase(d12) }, { BuyPayment(listOf(BuyDieResource(20, 13))) })
        val shadows = mutableListOf<Stub>()
        val observer = ShadowBuyStrategy(actual, {
            Stub({ BuyChoice.Purchase(d12) }, { payment }).also(shadows::add)
        }, {})
        observer.choosePurchase(ChoosePurchaseRequest(listOf(d12), purchasesMadeThisBuy = 0))
        observer.choosePayment(ChoosePaymentRequest(d12, listOf(BuyDieResource(20, 12), BuyDieResource(20, 13)), emptyList()))
        observer.choosePurchase(ChoosePurchaseRequest(listOf(d12), purchasesMadeThisBuy = 1))
        assertEquals(2, shadows.size)
    }

    @Test fun `diagnostic does not change returned actual choices and payments`() {
        val actual = Stub({ BuyChoice.Done }, { payment })
        val observer = ShadowBuyStrategy(actual, { Stub({ BuyChoice.Purchase(d12) }) }, {})
        assertEquals(BuyChoice.Done, observer.choosePurchase(ChoosePurchaseRequest(listOf(d12))))
        assertSame(payment, observer.choosePayment(ChoosePaymentRequest(d12, emptyList(), emptyList())))
    }
}
