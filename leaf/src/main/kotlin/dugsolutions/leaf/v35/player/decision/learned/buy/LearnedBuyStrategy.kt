package dugsolutions.leaf.v35.player.decision.learned.buy

import dugsolutions.leaf.v35.player.decision.buy.*

/** Learned choice of purchase only. Human Baseline still owns payment behavior. */
class LearnedBuyStrategy(
    weights: LearnedBuyWeights,
    private val paymentDelegate: BuyStrategy
) : BuyStrategy {
    private val scorer = LinearBuyActionScorer(weights)

    override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
        val candidates: List<BuyItem?> = request.options + listOf(null)
        // Stable list order is the deterministic tie-breaker. Done is deliberately last.
        val best = candidates.maxByOrNull { scorer.score(BuyFeatureExtractor.extract(request.context, it)).total }
        return best?.let { BuyChoice.Purchase(it) } ?: BuyChoice.Done
    }

    override fun choosePayment(request: ChoosePaymentRequest): BuyPayment = paymentDelegate.choosePayment(request)
}
