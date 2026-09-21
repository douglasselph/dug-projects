package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Scores Compost from the best permanent die upgrade currently available.
 *
 * [normalPurchasingPower] is supplied by the injected Human Baseline policy so
 * protected Critters are not silently treated as ordinary spending power.
 * [developmentBonus] is likewise supplied by policy and is intentionally a
 * modest nudge for being behind the long-term dice-development curve.
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

        val hand = context.self.board.hand
        val available = context.grove.graftBed.filterValues { it > 0 }.keys
        val candidates = hand.mapNotNull { die ->
            val current = DieSides.entries.firstOrNull { it.value == die.sides } ?: return@mapNotNull null
            val next = DieSides.entries.dropWhile { it != current }.drop(1).firstOrNull { it in available }
                ?: return@mapNotNull null
            Triple(die, current, next)
        }
        val best = candidates.maxByOrNull { (_, current, next) -> next.value - current.value }
            ?: return PriorityScore(NO_TARGET_SCORE)

        val (die, current, next) = best
        val upgrade = next.value - current.value
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
        val afterRemovingCurrentDie = (normalPurchasingPower - die.value).coerceAtLeast(0)
        val thresholdLoss = PurchaseThresholdHeuristics.thresholdBonus(
            beforePower = normalPurchasingPower,
            afterPower = afterRemovingCurrentDie,
            costs = tiers,
            pointsPerTier = POINTS_PER_BUY_TIER
        )

        var score = PriorityScore(BASE_SCORE)
            .adjusted(upgrade * POINTS_PER_UPGRADE_SIDE, "Permanent D${current.value} to D${next.value} upgrade")
            .adjusted(thresholdLoss, "Current-round Buy threshold impact")
            .adjusted(
                (context.progress.cultivationRoundsRemaining ?: 0)
                    .coerceAtMost(MAX_FUTURE_ROUNDS_BONUS_ROUNDS) * POINTS_PER_FUTURE_CULTIVATION_ROUND,
                "Future rounds benefit from upgrade"
            )

        if (developmentBonus > 0) {
            score = score.adjusted(developmentBonus, "Dice development is behind target")
        }
        return score
    }
}
