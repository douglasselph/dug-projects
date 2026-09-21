package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.ReserveResource
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.tokens.Critter

/** Favors minimum sufficient payment while preserving the shared Human Baseline Critter reserve. */
object PaymentPriority {
    fun score(
        context: DecisionContext,
        payment: BuyPayment,
        cost: Int,
        policy: HumanBaselinePolicy = HumanBaselinePolicy()
    ): PriorityScore {
        val overpay = payment.total - cost
        var score = PriorityScore(base = 100)
            .adjusted(-overpay * 6, "Avoid overpayment")
            .adjusted(-(payment.dice.size + payment.critters.size), "Prefer fewer physical resources")

        val reserve = policy.protectedCritterReserve(context)
        val beeSpend = payment.critters.count { it.critter == Critter.BEE }
        val wormSpend = payment.critters.count { it.critter == Critter.WORM }
        if (beeSpend > 0) {
            val status = ResourceReserveHeuristics.status(
                context.self.board,
                ReserveResource.BEE,
                reserve
            )
            score = score.adjusted(
                ResourceReserveHeuristics.spendPenalty(status, beeSpend, 20),
                "Preserve Bee reserve"
            )
        }
        if (wormSpend > 0) {
            val status = ResourceReserveHeuristics.status(
                context.self.board,
                ReserveResource.WORM,
                reserve
            )
            score = score.adjusted(
                ResourceReserveHeuristics.spendPenalty(status, wormSpend, 25),
                "Preserve Worm reserve"
            )
        }
        return score
    }

    fun tags(payment: BuyPayment): Set<DecisionTag> = buildSet {
        if (payment.critters.any { it.critter == Critter.BEE }) add(DecisionTag.SPEND_BEE)
        if (payment.critters.any { it.critter == Critter.WORM }) add(DecisionTag.SPEND_WORM)
    }
}
