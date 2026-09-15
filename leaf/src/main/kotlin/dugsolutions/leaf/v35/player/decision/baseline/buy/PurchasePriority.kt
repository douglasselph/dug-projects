package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.common.GraftTopologyEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.context.BaselineFeatureCalculator
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Generic Human Baseline purchase score; card-specific value arrives in Step 7. */
object PurchasePriority {
    fun score(
        context: DecisionContext,
        item: BuyItem,
        features: dugsolutions.leaf.v35.player.decision.baseline.context.BaselineFeatures =
            BaselineFeatureCalculator().calculate(context)
    ): PriorityScore {
        var score = PriorityScore(base = item.cost * 10)

        when (item) {
            is BuyItem.Die -> {
                if (features.dicePowerDeficit > 0) {
                    score = score.adjusted(
                        minOf(30, 5 + features.dicePowerDeficit),
                        "Dice supply is below its development target"
                    )
                }
            }

            is BuyItem.Plant -> {
                if (features.plantDeficit > 0) {
                    score = score.adjusted(
                        minOf(30, 10 + features.plantDeficit * 5),
                        "Creature is below its Plant development target"
                    )
                }
                val duplicates = context.self.board.creature.count { it.name == item.card.name }
                if (duplicates > 0) {
                    score = score.adjusted(-10 * duplicates.coerceAtMost(3), "Duplicate Plant penalty")
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
