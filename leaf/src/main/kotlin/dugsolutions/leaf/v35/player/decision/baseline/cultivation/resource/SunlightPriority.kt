package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView

/**
 * Scores the best immediate +3 Raise opportunity offered by Sunlight.
 *
 * [targetScore] is shared with the later Effect decision so Sunlight is applied
 * to the same kind of die that justified selecting the Round Effect.
 */
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

        val bestTarget = context.self.board.hand
            .map { die -> targetScore(context, die, normalPurchasingPower) }
            .maxByOrNull { it.total }
            ?: PriorityScore(0)

        return PriorityScore(
            base = BASE_SCORE,
            adjustments = bestTarget.adjustments
        )
    }

    /** Target-specific Sunlight value shared by action scoring and Effect choice. */
    fun targetScore(
        context: DecisionContext,
        die: DieView,
        normalPurchasingPower: Int
    ): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }

        val gain = DieValueHeuristics.actualRaiseGain(die, RAISE_AMOUNT)
        return PriorityScore(0)
            .adjusted(gain * POINTS_PER_ACTUAL_GAIN, "Actual +3 value gain")
            .adjusted(
                PurchaseThresholdHeuristics.thresholdBonus(
                    beforePower = normalPurchasingPower,
                    afterPower = normalPurchasingPower + gain,
                    costs = PurchaseThresholdHeuristics.availableCostTiers(context.grove),
                    pointsPerTier = POINTS_PER_BUY_TIER
                ),
                "Buy threshold gained by Sunlight"
            )
    }
}
