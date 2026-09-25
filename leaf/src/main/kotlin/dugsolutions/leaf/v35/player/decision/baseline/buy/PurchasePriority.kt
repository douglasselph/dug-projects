package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.GraftTopologyEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Generic Human Baseline purchase score, including card-specific acquire value. */
object PurchasePriority {
    fun score(
        context: DecisionContext,
        item: BuyItem,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): PriorityScore {
        var score = PriorityScore(base = item.cost * 10)

        when (item) {
            is BuyItem.Die -> Unit

            is BuyItem.Plant -> {
                val cardValue = cardScorers.forPlant(item.card).acquireScore(context, item.card)
                score = cardValue.adjustments.fold(score.adjusted(cardValue.base, "Card acquire value")) { acc, adjustment ->
                    acc.adjusted(adjustment)
                }
                if (item.card.type == PlantType.FLOWER &&
                    GraftTopologyEvaluator.wouldFlowerConsumeLastGrowthSlot(context.self.board.creature) &&
                    !context.progress.isFinalCultivationRound
                ) {
                    score = score.adjusted(-100, "Flower would consume the final growth slot")
                }
            }
        }
        return score
    }
}
