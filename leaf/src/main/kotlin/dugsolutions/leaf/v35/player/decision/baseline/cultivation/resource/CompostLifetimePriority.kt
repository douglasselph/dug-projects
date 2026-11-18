package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.cultivation.DrawPriority
import dugsolutions.leaf.v35.player.decision.baseline.projection.CompostLifetimeTarget
import dugsolutions.leaf.v35.player.decision.baseline.projection.CompostLifetimeValuation
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import kotlin.math.roundToInt

/**
 * Temporary legacy-scale adapter ONLY for ordinary v38 Round Compost.
 * Both candidate action and downstream Effect target use one pure forecast.
 * The Draw score supplies the legacy origin while DIFFERENCES are derived from
 * expected current+future pip equivalents; there is no fixed Compost base.
 * The modest Buy-tier penalty is exposed separately, never disguised as pips.
 */
object CompostLifetimePriority {
    private const val LEGACY_POINTS_PER_PIP = 4 // Same scale used by LegacyPreferenceBridge.cultivationDraw.
    private const val LOST_BUY_TIER_PENALTY = 10
    private const val NO_HAND_SCORE = -1000

    data class TargetEvaluation(val value: CompostLifetimeTarget, val score: PriorityScore)

    fun bestTarget(context: DecisionContext, normalPurchasingPower: Int): TargetEvaluation? {
        val comparison = CompostLifetimeValuation.compare(context, normalPurchasingPower)
        val draw = comparison.drawReference
        val base = DrawPriority.score(context).total
        return comparison.targets.map { target ->
            TargetEvaluation(target, targetScoreFromComparison(target, draw, base))
        }.maxByOrNull { it.score.total }
    }

    fun score(context: DecisionContext, normalPurchasingPower: Int): PriorityScore =
        bestTarget(context, normalPurchasingPower)?.score
            ?: PriorityScore(NO_HAND_SCORE).adjusted(0, "No Hand die available to Compost")

    /** This SAME target score is used after selecting Round Compost. */
    fun targetScore(
        context: DecisionContext,
        die: DieView,
        normalPurchasingPower: Int
    ): PriorityScore? {
        val comparison = CompostLifetimeValuation.compare(context, normalPurchasingPower)
        val target = comparison.targets.firstOrNull { it.die == die } ?: return null
        return targetScoreFromComparison(target, comparison.drawReference, DrawPriority.score(context).total)
    }

    private fun targetScoreFromComparison(
        target: CompostLifetimeTarget,
        drawReference: Double,
        legacyDrawScore: Int
    ): PriorityScore {
        val operation = if (target.isTrash) "Trash D${target.die.sides}" else
            "Upgrade D${target.die.sides} to D${target.replacementSides}"
        val pipDelta = target.modeledPipEquivalent - drawReference
        return PriorityScore(legacyDrawScore)
            .adjusted((pipDelta * LEGACY_POINTS_PER_PIP).roundToInt(),
                "$operation versus Draw: ${target.modeledPipEquivalent} vs $drawReference modeled " +
                    "current/future pip-equivalents (current loss ${target.currentSpendingPipsLost}, " +
                    "future spending ${target.futureSpendingPipsChange}, " +
                    "future rewards ${target.futureRewardEquivalentChange})")
            .adjusted(-LOST_BUY_TIER_PENALTY * target.lostAffordableBuyTiers.size,
                "Same-round affordable Buy tiers lost: ${target.lostAffordableBuyTiers.joinToString().ifEmpty { "none" }}")
    }
}
