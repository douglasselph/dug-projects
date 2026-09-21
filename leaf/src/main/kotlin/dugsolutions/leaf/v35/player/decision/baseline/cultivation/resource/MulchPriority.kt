package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView

/**
 * Scores the best currently available die for the Mulch Round Effect.
 *
 * [targetScore] is also used by the downstream Effect strategy so the die that
 * makes Mulch attractive is the die Human Baseline subsequently stores.
 */
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

        val bestTarget = context.self.board.hand
            .map { die -> targetScore(context, die, normalPurchasingPower) }
            .maxByOrNull { it.total }
            ?: return PriorityScore(NO_TARGET_SCORE)

        var score = PriorityScore(
            base = BASE_SCORE,
            adjustments = bestTarget.adjustments
        )
        val stored = context.self.board.mulch.size + context.self.board.pendingMulch.size
        if (stored < DESIRED_PREPARED_MULCH) {
            score = score.adjusted(BELOW_RESERVE_BONUS, "Fewer than two Mulched dice prepared")
        }
        return score
    }

    /** Target-specific Mulch value shared by action scoring and Effect choice. */
    fun targetScore(
        context: DecisionContext,
        die: DieView,
        normalPurchasingPower: Int
    ): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }

        var score = PriorityScore(0)
        if (die.value <= 2) {
            score = score.adjusted(LOW_ROLL_BONUS, "Low showing die is attractive to store")
        }
        score = score.adjusted(
            (die.sides / HIGH_SIDED_BONUS_DIVISOR).coerceAtMost(HIGH_SIDED_BONUS_CAP),
            "Higher-sided stored die has future value"
        )

        val after = (normalPurchasingPower - die.value).coerceAtLeast(0)
        return score.adjusted(
            PurchaseThresholdHeuristics.thresholdBonus(
                beforePower = normalPurchasingPower,
                afterPower = after,
                costs = PurchaseThresholdHeuristics.availableCostTiers(context.grove),
                pointsPerTier = POINTS_PER_BUY_TIER
            ),
            "Current-round Buy threshold impact"
        )
    }
}
