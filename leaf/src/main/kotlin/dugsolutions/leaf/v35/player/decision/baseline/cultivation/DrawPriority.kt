package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

object DrawPriority {
    fun score(context: DecisionContext): PriorityScore {
        val nextSides = context.self.board.supply.minOfOrNull { it.sides }
            ?: context.self.board.discard.minOfOrNull { it.sides }
            ?: return PriorityScore(20)
        val expected = DieValueHeuristics.expectedRoll(nextSides)
        return PriorityScore(35 + (4 * expected).roundToInt())
            .adjusted(0, "Expected next draw is D$nextSides")
    }
}
