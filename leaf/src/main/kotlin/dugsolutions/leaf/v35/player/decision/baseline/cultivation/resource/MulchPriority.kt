package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView

/**
 * Scores the best currently available die for the Mulch Round Effect.
 *
 * Human Baseline only voluntarily Mulches dice showing 1 through 4. The actual
 * willingness percentage is supplied by HumanBaselinePolicy; this scorer keeps
 * target choice aligned with the action-level gate and prevents a 5+ die from
 * being selected after the player decided that Mulch was worth considering.
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
    private const val NEVER_MULCH_SCORE = -1000

    fun score(
        context: DecisionContext,
        normalPurchasingPower: Int
    ): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }

        val bestTarget = preferredTarget(context, normalPurchasingPower)
            ?: return PriorityScore(NO_TARGET_SCORE)
        var score = PriorityScore(
            base = BASE_SCORE,
            adjustments = targetScore(context, bestTarget, normalPurchasingPower).adjustments
        )
        val stored = context.self.board.mulch.size + context.self.board.pendingMulch.size
        if (stored < DESIRED_PREPARED_MULCH) {
            score = score.adjusted(BELOW_RESERVE_BONUS, "Fewer than two Mulched dice prepared")
        }
        return score
    }

    /** The target that the downstream Effect strategy should also prefer. */
    fun preferredTarget(
        context: DecisionContext,
        normalPurchasingPower: Int
    ): DieView? {
        val eligible = context.self.board.hand.filter { it.value in 1..4 }
        val lowestValue = eligible.minOfOrNull { it.value } ?: return null
        return eligible
            .filter { it.value == lowestValue }
            .maxByOrNull { targetScore(context, it, normalPurchasingPower).total }
    }

    /** Target-specific Mulch value shared by action scoring and Effect choice. */
    fun targetScore(
        context: DecisionContext,
        die: DieView,
        normalPurchasingPower: Int
    ): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }
        if (die.value >= 5) {
            return PriorityScore(NEVER_MULCH_SCORE)
                .adjusted(0, "Human Baseline never voluntarily Mulches a die showing 5+")
        }

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
