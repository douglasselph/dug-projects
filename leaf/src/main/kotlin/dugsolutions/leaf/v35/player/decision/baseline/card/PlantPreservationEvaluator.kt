package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * General value of keeping one Plant as a permanent part of the Creature.
 *
 * This deliberately ignores Battle-grid tactics. It combines the scorer's
 * existing generic loss value (intrinsic usefulness plus projected end-game VP)
 * with card cost as a broad, printed proxy for the investment/power being lost.
 */
class PlantPreservationEvaluator(
    private val cardScorers: HumanBaselineCardScorerRegistry =
        HumanBaselineCardScorerRegistry()
) {
    operator fun invoke(
        context: DecisionContext,
        card: CreatureCardView
    ): Int {
        val scorer = cardScorers.forPlant(card)
        return scorer.lossValue(context, card) + card.cost * COST_WEIGHT
    }

    private companion object {
        const val COST_WEIGHT = 2
    }
}
