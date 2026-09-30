package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.CardScoringHelpers
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.GraftTopologyEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Generic Human Baseline purchase score, including card-specific acquire value. */
object PurchasePriority {
    fun score(
        context: DecisionContext,
        item: BuyItem,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
        policy: HumanBaselinePolicy = HumanBaselinePolicy()
    ): PriorityScore {
        var score = PriorityScore(base = item.cost * 10)

        when (item) {
            is BuyItem.Die -> Unit

            is BuyItem.Plant -> {
                var cardValue = cardScorers.forPlant(item.card).acquireScore(context, item.card)

                val projectedVp = CardScoringHelpers.projectedVp(context, item.card.scoringRule)
                if (projectedVp > 0) {
                    val desiredPointsPerVp = policy.buyPlantVpPointsPerProjectedVp(context)
                    val vpAdjustmentPerPoint = desiredPointsPerVp - DEFAULT_CARD_ACQUIRE_VP_POINTS_PER_VP
                    if (vpAdjustmentPerPoint != 0) {
                        cardValue = cardValue.adjusted(
                            projectedVp * vpAdjustmentPerPoint,
                            if (policy.isEndGamePlantBuyingWindow(context)) {
                                "End-game VP emphasis"
                            } else {
                                "Early/midgame VP discount"
                            }
                        )
                    }
                }

                val copiesOwned = context.self.board.creature.count { it.name == item.card.name }
                if (copiesOwned > 0) {
                    val penaltyPerCopy = policy.buyPlantDiversityPenaltyPerOwnedCopy(
                        context = context,
                        currentPurchaseValue = cardValue.total
                    )
                    val totalPenalty = copiesOwned * penaltyPerCopy
                    if (totalPenalty > 0) {
                        cardValue = cardValue.adjusted(
                            -totalPenalty,
                            "Diversify from $copiesOwned owned cop${if (copiesOwned == 1) "y" else "ies"}"
                        )
                    }
                }

                score = cardValue.adjustments.fold(
                    score.adjusted(cardValue.base, "Card acquire value")
                ) { acc, adjustment ->
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

    fun tags(item: BuyItem): Set<DecisionTag> = when (item) {
        is BuyItem.Die -> emptySet()
        is BuyItem.Plant -> when (item.card.type) {
            PlantType.ROOT -> setOf(DecisionTag.ACQUIRE_ROOT)
            PlantType.VINE -> setOf(DecisionTag.ACQUIRE_VINE)
            PlantType.FLOWER -> setOf(DecisionTag.ACQUIRE_FLOWER)
        }
    }

    /** CardScoringHelpers.acquireScore currently contributes three points per projected VP. */
    private const val DEFAULT_CARD_ACQUIRE_VP_POINTS_PER_VP: Int = 3
}
