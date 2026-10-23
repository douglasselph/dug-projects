package dugsolutions.leaf.v35.player.decision.buy.diagnostics

import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** D8: diagnostics must never alter the actual policy's trace or strategy RNG. */
class BuyDiagnosticNoninterferenceTest {
    private val d12 = BuyItem.Die(DieSides.D12)
    private val d20 = BuyItem.Die(DieSides.D20)

    private class CountingRandom(seed: Long) : StrategyRandomizer {
        private val underlying = StrategyRandomizer.create(seed)
        var draws = 0
        override fun nextInt(until: Int): Int {
            draws++
            return underlying.nextInt(until)
        }
    }

    private class RandomBuyPolicy(private val rng: StrategyRandomizer) : BuyStrategy {
        override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice =
            if (rng.nextInt(3) == 0) BuyChoice.Done
            else BuyChoice.Purchase(request.options[rng.nextInt(request.options.size)])

        override fun choosePayment(request: ChoosePaymentRequest): BuyPayment =
            if (rng.nextInt(2) == 0) BuyPayment(listOf(request.availableDice.first()))
            else BuyPayment(listOf(request.availableDice.last()))
    }

    private fun trace(policy: BuyStrategy): List<String> = buildList {
        // Identical successive synthetic Buy states, including a new phase.
        for (phase in 0..2) {
            for (position in 0..4) {
                val choice = policy.choosePurchase(
                    ChoosePurchaseRequest(listOf(d12, d20), purchasesMadeThisBuy = position)
                )
                add("$phase/$position:$choice")
                if (choice is BuyChoice.Purchase) {
                    val payment = policy.choosePayment(
                        ChoosePaymentRequest(choice.item,
                            listOf(BuyDieResource(20, 12), BuyDieResource(20, 20)), emptyList())
                    )
                    add("paid:${payment.dice.map { it.value }}")
                }
            }
        }
    }

    @Test fun `shadow comparisons and D3 events preserve actual choices payments and RNG`() {
        val controlRandom = CountingRandom(19573)
        val observedRandom = CountingRandom(19573)
        val expected = trace(RandomBuyPolicy(controlRandom))
        val comparisons = mutableListOf<BuyRecommendationComparison>()
        val events = mutableListOf<BuyDecisionEvent>()
        val observed = trace(ShadowBuyStrategy(
            actual = RandomBuyPolicy(observedRandom),
            shadowFactory = { RandomBuyPolicy(StrategyRandomizer.create(987654321L)) },
            onComparison = comparisons::add,
            onEvent = events::add
        ))
        assertEquals(expected, observed, "Observer must preserve every actual Buy choice/payment")
        assertEquals(controlRandom.draws, observedRandom.draws,
            "Shadow and event recording must not consume actual strategy random draws")
        assertEquals(15, comparisons.size)
        assertEquals(15, events.size)
        assertEquals((1L..15L).toList(), events.map { it.sequenceInObserver })
        assertTrue(events.zip(comparisons).all { (event, comparison) -> event.kind == comparison.kind })
    }

    @Test fun `shadow disagreement cannot execute a hypothetical payment or affect actual payment`() {
        var shadowPayments = 0
        val actualPayment = BuyPayment(listOf(BuyDieResource(20, 20)))
        val observed = ShadowBuyStrategy(
            actual = object : BuyStrategy {
                override fun choosePurchase(request: ChoosePurchaseRequest) = BuyChoice.Purchase(d20)
                override fun choosePayment(request: ChoosePaymentRequest) = actualPayment
            },
            shadowFactory = { object : BuyStrategy {
                override fun choosePurchase(request: ChoosePurchaseRequest) = BuyChoice.Purchase(d12)
                override fun choosePayment(request: ChoosePaymentRequest): BuyPayment {
                    shadowPayments++
                    return BuyPayment()
                }
            } }, onComparison = {}
        )
        assertEquals(BuyChoice.Purchase(d20), observed.choosePurchase(ChoosePurchaseRequest(listOf(d12, d20))))
        assertSame(actualPayment, observed.choosePayment(
            ChoosePaymentRequest(d20, listOf(BuyDieResource(20, 20)), emptyList())
        ))
        assertEquals(0, shadowPayments)
    }
}
