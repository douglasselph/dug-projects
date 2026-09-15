package dugsolutions.leaf.v35.player.decision.baseline.placement

import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.baseline.common.GraftTopologyEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.abs

/** Scores legal physical graft positions without considering card-specific strategy. */
object GraftPlacementPriority {
    fun score(context: DecisionContext, type: PlantType, placement: GraftPlacement): PriorityScore {
        val creature = context.self.board.creature
        val left = creature.count { it.side == CreatureSide.LEFT }
        val right = creature.count { it.side == CreatureSide.RIGHT }
        val afterLeft = left + if (placement.side == CreatureSide.LEFT) 1 else 0
        val afterRight = right + if (placement.side == CreatureSide.RIGHT) 1 else 0
        val imbalance = abs(afterLeft - afterRight)

        var score = PriorityScore(base = 60)
            .adjusted(-10 * imbalance, "Keep the Creature balanced")

        val futureSlots = GraftTopologyEvaluator.futureGrowthSlotsAfter(creature, type, placement)
        score = score.adjusted(
            futureSlots * 4,
            "Preserve future growth locations"
        )

        if (type == PlantType.FLOWER &&
            GraftTopologyEvaluator.wouldFlowerConsumeLastGrowthSlot(creature, placement) &&
            !context.progress.isFinalCultivationRound
        ) {
            score = score.adjusted(-100, "Do not consume the last growth slot")
        }
        return score
    }
}
