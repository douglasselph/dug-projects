package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/**
 * Scores the immediate value of drawing the next die during Cultivation.
 *
 * Draw is deliberately about the die becoming available this round. It does
 * not receive the long-term dice-development deficit bonus because moving an
 * existing die from Supply/Discard to Hand does not increase total dice-pool
 * power. Permanent development nudges belong on actions such as Compost (and
 * on later Buy decisions that actually acquire/upgrade dice).
 */
object DrawPriority {
    private const val NO_DIE_SCORE = 20
    private const val BASE_SCORE = 35
    private const val POINTS_PER_EXPECTED_VALUE = 4

    fun score(context: DecisionContext): PriorityScore {
        val nextSides = context.self.board.supply.minOfOrNull { it.sides }
            ?: context.self.board.discard.minOfOrNull { it.sides }
            ?: return PriorityScore(NO_DIE_SCORE)
        val expected = DieValueHeuristics.expectedRoll(nextSides)
        return PriorityScore(BASE_SCORE + (POINTS_PER_EXPECTED_VALUE * expected).roundToInt())
            .adjusted(0, "Expected next draw is D$nextSides")
    }
}
