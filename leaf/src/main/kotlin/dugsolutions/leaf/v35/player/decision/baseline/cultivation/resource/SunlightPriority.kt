package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView

/**
 * Scores the immediate +3 Raise offered by Sunlight.
 *
 * For ordinary Human Baseline play, Sunlight is only worth considering when a
 * full +3 on a Hand die beats the expected value of drawing the next die. With
 * the current dice ladder that means the next draw must be a D4 and the chosen
 * target must actually gain all 3 (so D6=4 -> 6, a gain of only 2, never
 * qualifies). The 50% willingness gate itself lives in HumanBaselinePolicy.
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

        val bestTarget = preferredTarget(context)
            ?: return PriorityScore(0)

        return PriorityScore(
            base = BASE_SCORE,
            adjustments = targetScore(context, bestTarget, normalPurchasingPower).adjustments
        )
    }

    /** True only when Sunlight's best actual gain beats the expected next draw. */
    fun isWorthConsidering(context: DecisionContext): Boolean {
        val nextSides = context.self.board.supply.minOfOrNull { it.sides }
            ?: context.self.board.discard.minOfOrNull { it.sides }
            ?: return false
        val expectedDraw = DieValueHeuristics.expectedRoll(nextSides)
        val bestGain = context.self.board.hand.maxOfOrNull {
            DieValueHeuristics.actualRaiseGain(it, RAISE_AMOUNT)
        } ?: return false
        return bestGain > expectedDraw
    }

    /** Prefer a target receiving the largest actual raise; ties use the normal target score. */
    fun preferredTarget(context: DecisionContext): DieView? {
        val bestGain = context.self.board.hand.maxOfOrNull {
            DieValueHeuristics.actualRaiseGain(it, RAISE_AMOUNT)
        } ?: return null
        return context.self.board.hand
            .filter { DieValueHeuristics.actualRaiseGain(it, RAISE_AMOUNT) == bestGain }
            .maxByOrNull { it.sides }
    }

    /** Target-specific Sunlight value shared by action scoring and Effect choice. */
    fun targetScore(
        context: DecisionContext,
        die: DieView,
        normalPurchasingPower: Int
    ): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }

        val gain = DieValueHeuristics.actualRaiseGain(die, RAISE_AMOUNT)
        val bestGain = context.self.board.hand.maxOfOrNull {
            DieValueHeuristics.actualRaiseGain(it, RAISE_AMOUNT)
        } ?: gain
        if (gain < bestGain) {
            return PriorityScore(-1000)
                .adjusted(0, "Prefer the die receiving the full visible Sunlight gain")
        }
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
