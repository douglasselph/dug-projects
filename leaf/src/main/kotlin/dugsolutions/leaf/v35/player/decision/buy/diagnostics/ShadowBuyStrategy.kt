package dugsolutions.leaf.v35.player.decision.buy.diagnostics

import dugsolutions.leaf.v35.player.decision.baseline.buy.HumanBaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer

/**
 * Observes the ACTUAL strategy's Buy decisions, without changing its choices or payments.
 *
 * The shadow baseline has its own RNG and its own multi-purchase plan. A planned
 * continuation remains meaningful only while the actual purchases AND payments
 * follow the shadow. When they diverge, the next comparison uses a fresh Human
 * Baseline planner on the next real decision state. In particular, a baseline
 * hypothetical purchase is never executed on the actual player.
 *
 * D2 is a reusable opt-in decorator; evaluation telemetry/aggregation is D3-D7.
 * Install a separate instance per player, not one shared across players.
 */
class ShadowBuyStrategy(
    private val actual: BuyStrategy,
    private val shadowFactory: () -> BuyStrategy,
    private val onComparison: (BuyRecommendationComparison) -> Unit,
    private val onEvent: (BuyDecisionEvent) -> Unit = {}
) : BuyStrategy {
    private var shadow: BuyStrategy? = null
    private var lastSelectedSameItem = false
    private var lastPurchaseCount: Int? = null
    private var awaitingPayment = false
    private var eventSequence = 0L

    override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
        // A fresh Buy phase begins at zero. A skipped payment, repeated decision,
        // or unexpected purchase-count transition invalidates a cached plan.
        if (request.purchasesMadeThisBuy == 0 || awaitingPayment ||
            (lastPurchaseCount?.let { request.purchasesMadeThisBuy != it + 1 } == true)
        ) invalidate()

        val baseline = (shadow ?: shadowFactory().also { shadow = it }).choosePurchase(request)
        val expert = actual.choosePurchase(request)
        val comparison = BuyDisagreement.compare(baseline, expert)
        onComparison(comparison)
        onEvent(BuyDecisionEvent.capture(++eventSequence, request, baseline, expert))
        lastSelectedSameItem = comparison.kind == BuyDisagreementKind.SAME_ITEM &&
            expert is BuyChoice.Purchase
        awaitingPayment = expert is BuyChoice.Purchase
        lastPurchaseCount = request.purchasesMadeThisBuy
        if (!lastSelectedSameItem) shadow = null
        return expert
    }

    override fun choosePayment(request: ChoosePaymentRequest): BuyPayment {
        // Never allow diagnostics to affect which payment the actual strategy makes.
        val actualPayment = actual.choosePayment(request)
        if (lastSelectedSameItem) {
            val shadowPayment = shadow?.choosePayment(request)
            if (shadowPayment == null || !samePayment(shadowPayment, actualPayment)) {
                shadow = null
            }
        } else {
            shadow = null
        }
        awaitingPayment = false
        lastSelectedSameItem = false
        return actualPayment
    }

    private fun invalidate() {
        shadow = null
        lastSelectedSameItem = false
        awaitingPayment = false
        lastPurchaseCount = null
    }

    private fun samePayment(a: BuyPayment, b: BuyPayment): Boolean =
        a.dice.groupingBy { it }.eachCount() == b.dice.groupingBy { it }.eachCount() &&
            a.critters.groupingBy { it }.eachCount() == b.critters.groupingBy { it }.eachCount()

    companion object {
        /** Explicit seed avoids consuming actual game or strategy RNG to seed the shadow. */
        fun humanBaseline(
            actual: BuyStrategy,
            shadowSeed: Long,
            onComparison: (BuyRecommendationComparison) -> Unit,
            onEvent: (BuyDecisionEvent) -> Unit = {}
        ): ShadowBuyStrategy = ShadowBuyStrategy(
            actual = actual,
            shadowFactory = {
                HumanBaselineBuyStrategy(strategyRandomizer = StrategyRandomizer.create(shadowSeed))
            },
            onComparison = onComparison,
            onEvent = onEvent
        )
    }
}
