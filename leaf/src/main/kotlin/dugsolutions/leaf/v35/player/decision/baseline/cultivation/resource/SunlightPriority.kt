package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Scores the best immediate +3 Raise opportunity offered by Sunlight. */
object SunlightPriority {
    private const val BASE_SCORE = 35
    private const val RAISE_AMOUNT = 3
    private const val POINTS_PER_ACTUAL_GAIN = 5
    private const val POINTS_PER_BUY_TIER = 10

    fun score(
        context: DecisionContext,
        normalPurchasingPower: Int
    ): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }

        val best = context.self.board.hand.maxOfOrNull {
            DieValueHeuristics.actualRaiseGain(it, RAISE_AMOUNT)
        } ?: 0
        var score = PriorityScore(BASE_SCORE + POINTS_PER_ACTUAL_GAIN * best)
        score = score.adjusted(
            PurchaseThresholdHeuristics.thresholdBonus(
                beforePower = normalPurchasingPower,
                afterPower = normalPurchasingPower + best,
                costs = PurchaseThresholdHeuristics.availableCostTiers(context.grove),
                pointsPerTier = POINTS_PER_BUY_TIER
            ),
            "Buy threshold gained by Sunlight"
        )
        return score
    }
}
