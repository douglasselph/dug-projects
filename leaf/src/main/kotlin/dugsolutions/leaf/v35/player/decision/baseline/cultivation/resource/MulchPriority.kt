package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Scores the best currently available die for the Mulch Round Effect. */
object MulchPriority {
    private const val NO_TARGET_SCORE = 10
    private const val BASE_SCORE = 45
    private const val LOW_ROLL_BONUS = 20
    private const val DESIRED_PREPARED_MULCH = 2
    private const val BELOW_RESERVE_BONUS = 10
    private const val HIGH_SIDED_BONUS_DIVISOR = 4
    private const val HIGH_SIDED_BONUS_CAP = 5
    private const val POINTS_PER_BUY_TIER = 10

    fun score(
        context: DecisionContext,
        normalPurchasingPower: Int
    ): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }

        val target = context.self.board.hand.maxByOrNull { die ->
            (if (die.value <= 2) 100 else 0) + die.sides - die.value * 4
        } ?: return PriorityScore(NO_TARGET_SCORE)

        var score = PriorityScore(BASE_SCORE)
        if (target.value <= 2) {
            score = score.adjusted(LOW_ROLL_BONUS, "Low showing die is attractive to store")
        }
        val stored = context.self.board.mulch.size + context.self.board.pendingMulch.size
        if (stored < DESIRED_PREPARED_MULCH) {
            score = score.adjusted(BELOW_RESERVE_BONUS, "Fewer than two Mulched dice prepared")
        }
        score = score.adjusted(
            (target.sides / HIGH_SIDED_BONUS_DIVISOR).coerceAtMost(HIGH_SIDED_BONUS_CAP),
            "Higher-sided stored die has future value"
        )

        val after = (normalPurchasingPower - target.value).coerceAtLeast(0)
        score = score.adjusted(
            PurchaseThresholdHeuristics.thresholdBonus(
                beforePower = normalPurchasingPower,
                afterPower = after,
                costs = PurchaseThresholdHeuristics.availableCostTiers(context.grove),
                pointsPerTier = POINTS_PER_BUY_TIER
            ),
            "Current-round Buy threshold impact"
        )
        return score
    }
}
