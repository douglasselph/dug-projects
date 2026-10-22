package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorer
import dugsolutions.leaf.v35.player.decision.baseline.common.GraftTopologyEvaluator
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/**
 * A deliberately small, Buy-only estimate of how likely a purchased Plant is to
 * be playable before the game ends. It is NOT an immediate activation score.
 *
 * Purchased Plants are grafted face down. An available round is an opportunity,
 * not a guaranteed activation; spending a Main Action or refreshing the Plant
 * still has a cost. End-game VP is handled separately by PurchasePriority.
 */
object PlantAcquisitionUsability {
    data class Estimate(
        val legalPlacements: Int,
        val futureCultivationRounds: Int?,
        val futureBattleRounds: Int?,
        val adjustment: Int
    )

    fun estimate(context: DecisionContext, card: PlantCard, scorer: HumanBaselineCardScorer): Estimate {
        val placements = GraftTopologyEvaluator.legalPlacements(context.self.board.creature, card.type).size
        val cultivation = context.progress.cultivationRoundsRemaining
        val battle = context.progress.battleRoundsRemaining
        if (placements == 0) return Estimate(0, cultivation, battle, -12)
        // Null round counts mean the schedule is unknown, not that there are zero opportunities.
        if (cultivation == null || battle == null) return Estimate(placements, cultivation, battle, 0)

        val cultivationOpportunity = cultivation.coerceAtLeast(0).coerceAtMost(3)
        val battleOpportunity = battle.coerceAtLeast(0).coerceAtMost(3)
        // Existing configured bases are card-specific estimates. These bounded
        // weights avoid using a tactical PLAY score as a BUY score.
        val cultivationQuality = (scorer.cultivationPlayBase - 50).coerceIn(0, 80) / 80.0
        val battleQuality = (scorer.battlePlayBase - 50).coerceIn(0, 80) / 80.0
        val weighted = cultivationOpportunity * cultivationQuality + battleOpportunity * battleQuality
        val adjustment = when {
            cultivationOpportunity + battleOpportunity == 0 -> -12
            else -> (weighted * 2.0).roundToInt().coerceIn(0, 12)
        }
        return Estimate(placements, cultivation, battle, adjustment)
    }
}
