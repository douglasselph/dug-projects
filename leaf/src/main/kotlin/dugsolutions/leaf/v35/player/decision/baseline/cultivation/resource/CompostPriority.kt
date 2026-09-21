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
 */
object CompostPriority {
    private const val NO_TARGET_SCORE = 15
    private const val BASE_SCORE = 75
    private const val POINTS_PER_UPGRADE_SIDE = 2
    private const val POINTS_PER_BUY_TIER = 10
    private const val POINTS_PER_FUTURE_CULTIVATION_ROUND = 2
    private const val MAX_FUTURE_ROUNDS_BONUS_ROUNDS = 5

    fun score(
        context: DecisionContext,
        normalPurchasingPower: Int,
        developmentBonus: Int
    ): PriorityScore {
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }
        require(developmentBonus >= 0) { "Development bonus cannot be negative" }

        val bestTarget = context.self.board.hand
            .mapNotNull { die -> targetScore(context, die, normalPurchasingPower) }
            .maxByOrNull { it.total }
            ?: return PriorityScore(NO_TARGET_SCORE)

        var score = PriorityScore(
            base = BASE_SCORE,
            adjustments = bestTarget.adjustments
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
        val thresholdLoss = PurchaseThresholdHeuristics.thresholdBonus(
            beforePower = normalPurchasingPower,
            afterPower = afterRemovingCurrentDie,
            costs = PurchaseThresholdHeuristics.availableCostTiers(context.grove),
            pointsPerTier = POINTS_PER_BUY_TIER
        )

        return PriorityScore(0)
            .adjusted(upgrade * POINTS_PER_UPGRADE_SIDE, "Permanent D${current.value} to D${next.value} upgrade")
            .adjusted(thresholdLoss, "Current-round Buy threshold impact")
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
