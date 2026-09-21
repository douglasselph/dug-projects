package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Scores acquiring Water as a reusable Cultivation/Battle support resource. */
object WaterPriority {
    private const val BASE_SCORE = 30
    private const val FIRST_WATER_BONUS = 20
    private const val SECOND_WATER_BONUS = 10
    private const val HEALTHY_RESERVE_PENALTY = -15

    val tags: Set<DecisionTag> = setOf(DecisionTag.ACQUIRE_WATER)

    fun score(context: DecisionContext): PriorityScore =
        when (context.self.board.water) {
            0 -> PriorityScore(BASE_SCORE).adjusted(FIRST_WATER_BONUS, "Acquire first Water")
            1 -> PriorityScore(BASE_SCORE).adjusted(SECOND_WATER_BONUS, "A second Water is still useful")
            else -> PriorityScore(BASE_SCORE).adjusted(HEALTHY_RESERVE_PENALTY, "Water reserve is already healthy")
        }
}
