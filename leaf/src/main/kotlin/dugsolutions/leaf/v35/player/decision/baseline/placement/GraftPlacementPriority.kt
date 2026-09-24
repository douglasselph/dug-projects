package dugsolutions.leaf.v35.player.decision.baseline.placement

import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.GraftTopologyEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Scores legal physical graft positions without considering card-specific strategy. */
object GraftPlacementPriority {
    private const val BASE_SCORE = 60
    private const val CONSTRAINED_TOPOLOGY_BONUS = 20
    private const val ADEQUATE_TOPOLOGY_BONUS = 40
    private const val LESS_DEVELOPED_SIDE_BONUS = 1
    private const val PREMATURE_LAST_SLOT_PENALTY = 100

    internal enum class GrowthBand {
        BOXED_IN,
        CONSTRAINED,
        ADEQUATE
    }

    fun score(
        context: DecisionContext,
        type: PlantType,
        placement: GraftPlacement,
        policy: HumanBaselinePolicy = HumanBaselinePolicy()
    ): PriorityScore {
        val creature = context.self.board.creature
        var score = PriorityScore(base = BASE_SCORE)

        // There is no later Cultivation in which preserved connector capacity can
        // be used, so final-Cultivation placement falls through to weak balance.
        if (!context.progress.isFinalCultivationRound) {
            val futureSlots = GraftTopologyEvaluator.futureGrowthSlotsAfter(creature, type, placement)
            val band = growthBand(futureSlots, policy.graftAdequateGrowthSlots(context))
            score = score.adjusted(
                topologyBonus(band),
                "Future growth is ${band.reasonLabel}"
            )

            if (type == PlantType.FLOWER &&
                GraftTopologyEvaluator.wouldFlowerConsumeLastGrowthSlot(creature, placement)
            ) {
                score = score.adjusted(
                    -PREMATURE_LAST_SLOT_PENALTY,
                    "Do not consume the last growth slot"
                )
            }
        }

        if (placement.side == lessDevelopedSide(creature)) {
            score = score.adjusted(
                LESS_DEVELOPED_SIDE_BONUS,
                "Prefer the less-developed side when topology is equivalent"
            )
        }
        return score
    }

    internal fun growthBand(futureSlots: Int, adequateThreshold: Int): GrowthBand {
        require(futureSlots >= 0) { "Future growth slots cannot be negative" }
        require(adequateThreshold >= 2) { "Adequate growth threshold must be at least 2" }
        return when {
            futureSlots == 0 -> GrowthBand.BOXED_IN
            futureSlots < adequateThreshold -> GrowthBand.CONSTRAINED
            else -> GrowthBand.ADEQUATE
        }
    }

    private fun topologyBonus(band: GrowthBand): Int = when (band) {
        GrowthBand.BOXED_IN -> 0
        GrowthBand.CONSTRAINED -> CONSTRAINED_TOPOLOGY_BONUS
        GrowthBand.ADEQUATE -> ADEQUATE_TOPOLOGY_BONUS
    }

    private fun lessDevelopedSide(
        creature: List<dugsolutions.leaf.v35.player.decision.context.CreatureCardView>
    ): CreatureSide? {
        val left = creature.count { it.side == CreatureSide.LEFT }
        val right = creature.count { it.side == CreatureSide.RIGHT }
        return when {
            left < right -> CreatureSide.LEFT
            right < left -> CreatureSide.RIGHT
            else -> null
        }
    }

    private val GrowthBand.reasonLabel: String
        get() = when (this) {
            GrowthBand.BOXED_IN -> "boxed in"
            GrowthBand.CONSTRAINED -> "constrained"
            GrowthBand.ADEQUATE -> "adequate"
        }
}
