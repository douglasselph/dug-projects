package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.context.DicePoolQuality
import dugsolutions.leaf.v35.player.decision.baseline.context.PhaseProximity
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView

/**
 * Human Baseline valuation of committing a Hand die to Mulch.
 *
 * Mulch is deliberately not an eligibility rule based on the current face.  A
 * low roll is attractive, but a high-sided die can still be worth preparing at
 * a middling face when Battle is imminent or the natural upcoming dice are
 * weak.  All constants here are calibration candidates, not designer-approved
 * card values.
 */
object MulchPriority {
    private const val NO_TARGET_SCORE = 10
    private const val BASE_SCORE = 45
    private const val DESIRED_PREPARED_MULCH = 2
    private const val BELOW_RESERVE_BONUS = 10
    private const val POINTS_PER_BUY_TIER = 10
    private const val BATTLE_NEXT_BONUS = 16
    private const val WEAK_UPCOMING_BATTLE_BONUS = 8
    private const val HIGH_SIDED_BATTLE_DIVISOR = 2
    private const val HIGH_SIDED_BATTLE_CAP = 10

    data class TimingObservation(
        val battleIsNext: Boolean,
        val preparedMulchCount: Int,
        val upcomingSupplySides: List<Int>,
        val upcomingSupplyAverageSides: Double?,
        val approximateNaturalRecycleDraws: Int
    )

    fun score(context: DecisionContext, normalPurchasingPower: Int): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }
        val bestTarget = preferredTarget(context, normalPurchasingPower)
            ?: return PriorityScore(NO_TARGET_SCORE)
        var score = PriorityScore(
            base = BASE_SCORE,
            adjustments = targetScore(context, bestTarget, normalPurchasingPower).adjustments
        )
        if (timingObservation(context, bestTarget).preparedMulchCount < DESIRED_PREPARED_MULCH) {
            score = score.adjusted(BELOW_RESERVE_BONUS, "Fewer than two Mulched dice prepared")
        }
        return score
    }

    fun preferredTarget(context: DecisionContext, normalPurchasingPower: Int): DieView? =
        context.self.board.hand.maxByOrNull { targetScore(context, it, normalPurchasingPower).total }

    fun targetScore(context: DecisionContext, die: DieView, normalPurchasingPower: Int): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }
        val timing = timingObservation(context, die)
        var score = PriorityScore(0)
            .adjusted(rollValue(die), "Graded current-roll value for Mulch")
            .adjusted(sizeValue(die), "Die size preserves future Mulch value")

        if (timing.battleIsNext) {
            score = score
                .adjusted(BATTLE_NEXT_BONUS, "Battle is next")
                .adjusted(
                    (die.sides / HIGH_SIDED_BATTLE_DIVISOR).coerceAtMost(HIGH_SIDED_BATTLE_CAP),
                    "High-sided die is especially useful when Battle is next"
                )
            val upcomingAverage = timing.upcomingSupplyAverageSides
            if (upcomingAverage != null && upcomingAverage < die.sides) {
                score = score.adjusted(
                    WEAK_UPCOMING_BATTLE_BONUS,
                    "Naturally upcoming dice are weaker than the Mulched die"
                )
            }
        }

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

    /** Approximate visible timing if this Hand die instead had to cycle naturally. */
    fun timingObservation(context: DecisionContext, die: DieView): TimingObservation {
        require(die in context.self.board.hand) { "Mulch timing observation requires a Hand die" }
        val quality = DicePoolQuality.observe(context)
        val lowerDiscardAhead = context.self.board.discard.count { candidate ->
            candidate.sides < die.sides || (candidate.sides == die.sides && candidate.index < die.index)
        }
        return TimingObservation(
            battleIsNext = PhaseProximity.battleIsNext(context),
            preparedMulchCount = quality.preparedMulchCount,
            upcomingSupplySides = quality.nextSupplySides,
            upcomingSupplyAverageSides = quality.nextSupplySides.takeIf { it.isNotEmpty() }?.average(),
            approximateNaturalRecycleDraws = context.self.board.supply.size + lowerDiscardAhead + 1
        )
    }

    /** Smoothly falls toward zero around a showing of five; higher faces remain legal. */
    private fun rollValue(die: DieView): Int = when (die.value) {
        1 -> 20
        2 -> 15
        3 -> 10
        4 -> 5
        5 -> 0
        else -> -(die.value - 5) * 2
    }

    /** A small persistent preference for preserving larger dice. */
    private fun sizeValue(die: DieView): Int = (die.sides - 4).coerceAtLeast(0) / 2
}
