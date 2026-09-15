package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

object WaterPriority {
    fun score(context: DecisionContext): PriorityScore =
        when (context.self.board.water) {
            0 -> PriorityScore(30).adjusted(20, "Acquire first Water")
            1 -> PriorityScore(30).adjusted(10, "A second Water is still useful")
            else -> PriorityScore(30).adjusted(-15, "Water reserve is already healthy")
        }
}
