package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Scores Compost from the best legal permanent die upgrade currently available.
 *
 * [normalPurchasingPower] is supplied by the injected Human Baseline policy so
 * protected Critters are not silently treated as ordinary spending power.
 * [developmentBonus] is likewise supplied by policy and is intentionally a
 * modest nudge for being behind the long-term dice-development curve.
 *
 * [targetScore] is shared with the downstream Effect decision. That is
 * deliberate: if Compost is attractive because one particular Hand die is the
 * best upgrade target, the later Effect choice must prefer that same target.
 *
 * Compost also models two ordinary-player guardrails that are intentionally
 * separate from raw permanent-upgrade value:
 *
 * - Players normally try to leave at least [MIN_HAND_DICE_BUY_POWER_AFTER_COMPOST]
 *   showing Hand-die power for Buy, so dropping below that floor receives a
 *   substantial (but not absolute) penalty.
 * - The top-level Cultivation strategy applies [usePercentage] as strategy
 *   variation: low showing dice are much more tempting to Compost, ordinary
 *   rolls are roughly a coin flip, and a second die commitment is rarer.
 */
object CompostPriority {
    private const val NO_TARGET_SCORE = 15
    private const val BASE_SCORE = 75
    private const val POINTS_PER_UPGRADE_SIDE = 2
    private const val POINTS_PER_BUY_TIER = 10
    private const val POINTS_PER_FUTURE_CULTIVATION_ROUND = 2
    private const val MAX_FUTURE_ROUNDS_BONUS_ROUNDS = 5

    /** Human Baseline normally wants to preserve enough showing Hand-die power for a cost-5 Root. */
    const val MIN_HAND_DICE_BUY_POWER_AFTER_COMPOST = 5

    /** Crossing below the normal cost-5 Hand-die Buy floor should usually make another action preferable. */
    private const val BELOW_HAND_DICE_BUY_POWER_PENALTY = -40

    /** Ordinary Compost use is intentionally probabilistic rather than automatic. */
    const val DEFAULT_USE_PERCENTAGE = 50
    const val LOW_SHOWING_DIE_USE_PERCENTAGE = 75
    const val BELOW_HAND_DICE_BUY_POWER_USE_PERCENTAGE = 10
    private const val REPEATED_DIE_COMMITMENT_DIVISOR = 2

    data class TargetEvaluation(
        val die: DieView,
        val score: PriorityScore,
        val handDicePowerAfter: Int
    )

    fun score(
        context: DecisionContext,
        normalPurchasingPower: Int,
        developmentBonus: Int
    ): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }
        require(developmentBonus >= 0) { "Development bonus cannot be negative" }

        val bestTarget = bestTarget(context, normalPurchasingPower)
            ?: return PriorityScore(NO_TARGET_SCORE)

        var score = PriorityScore(
            base = BASE_SCORE,
            adjustments = bestTarget.score.adjustments
        ).adjusted(
            (context.progress.cultivationRoundsRemaining ?: 0)
                .coerceAtMost(MAX_FUTURE_ROUNDS_BONUS_ROUNDS) * POINTS_PER_FUTURE_CULTIVATION_ROUND,
            "Future rounds benefit from upgrade"
        )

        if (developmentBonus > 0) {
            score = score.adjusted(developmentBonus, "Dice development is behind target")
        }
        return score
    }

    /** Best legal Compost target under the same valuation used by Effect choice. */
    fun bestTarget(
        context: DecisionContext,
        normalPurchasingPower: Int
    ): TargetEvaluation? {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }

        return context.self.board.hand
            .mapNotNull { die ->
                targetScore(context, die, normalPurchasingPower)?.let { score ->
                    TargetEvaluation(
                        die = die,
                        score = score,
                        handDicePowerAfter = (context.self.board.hand.sumOf { it.value } - die.value).coerceAtLeast(0)
                    )
                }
            }
            .maxByOrNull { it.score.total }
    }

    /**
     * Probability that an otherwise-competitive Compost action is followed by
     * an ordinary Human Baseline player on this decision opportunity.
     *
     * A 1 or 2 is especially attractive to remove from Hand (75%). Other
     * showing values are roughly a coin flip (50%). If Compost would leave less
     * than cost-5 Hand-die Buy power, it is rare (10%). Finally, when the player
     * is on the second Main Action with only two or fewer Hand dice left, the
     * percentage is halved; in normal play that is a strong signal that a die
     * was already committed to Compost/Mulch this Build.
     */
    fun usePercentage(
        context: DecisionContext,
        normalPurchasingPower: Int,
        mainActionsRemaining: Int
    ): Int {
        require(mainActionsRemaining in 1..2) {
            "Compost use percentage requires 1 or 2 Main Actions remaining: $mainActionsRemaining"
        }
        val target = bestTarget(context, normalPurchasingPower) ?: return 0

        var percentage = when {
            target.handDicePowerAfter < MIN_HAND_DICE_BUY_POWER_AFTER_COMPOST ->
                BELOW_HAND_DICE_BUY_POWER_USE_PERCENTAGE
            target.die.value <= 2 ->
                LOW_SHOWING_DIE_USE_PERCENTAGE
            else ->
                DEFAULT_USE_PERCENTAGE
        }

        val likelySecondDieCommitment =
            mainActionsRemaining == 1 && context.self.board.hand.size <= 2
        if (likelySecondDieCommitment) {
            percentage = (percentage / REPEATED_DIE_COMMITMENT_DIVISOR).coerceAtLeast(1)
        }
        return percentage
    }

    /**
     * Scores one concrete Compost target using only target-specific value.
     * Returns null when the die cannot make the exact normal one-step upgrade
     * that the real Upgrade rules require.
     */
    fun targetScore(
        context: DecisionContext,
        die: DieView,
        normalPurchasingPower: Int
    ): PriorityScore? {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }

        val current = DieSides.entries.firstOrNull { it.value == die.sides } ?: return null
        val next = nextNormalStep(current) ?: return null
        if ((context.grove.graftBed[next] ?: 0) <= 0) return null

        val upgrade = next.value - current.value
        val afterRemovingCurrentDie = (normalPurchasingPower - die.value).coerceAtLeast(0)
        val handDicePowerAfter = (context.self.board.hand.sumOf { it.value } - die.value).coerceAtLeast(0)
        val thresholdLoss = PurchaseThresholdHeuristics.thresholdBonus(
            beforePower = normalPurchasingPower,
            afterPower = afterRemovingCurrentDie,
            costs = PurchaseThresholdHeuristics.availableCostTiers(context.grove),
            pointsPerTier = POINTS_PER_BUY_TIER
        )

        var score = PriorityScore(0)
            .adjusted(upgrade * POINTS_PER_UPGRADE_SIDE, "Permanent D${current.value} to D${next.value} upgrade")
            .adjusted(thresholdLoss, "Current-round Buy threshold impact")

        if (handDicePowerAfter < MIN_HAND_DICE_BUY_POWER_AFTER_COMPOST) {
            score = score.adjusted(
                BELOW_HAND_DICE_BUY_POWER_PENALTY,
                "Preserve at least $MIN_HAND_DICE_BUY_POWER_AFTER_COMPOST Hand-die Buy power"
            )
        }
        return score
    }

    private fun nextNormalStep(sides: DieSides): DieSides? =
        when (sides) {
            DieSides.D4 -> DieSides.D6
            DieSides.D6 -> DieSides.D8
            DieSides.D8 -> DieSides.D10
            DieSides.D10 -> DieSides.D12
            DieSides.D12 -> DieSides.D20
            DieSides.D20 -> null
        }
}
