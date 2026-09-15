package dugsolutions.leaf.v35.player.decision.baseline.reward

import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.tokens.Critter

/** Human Baseline's intentionally simple Bee/Worm reward preference. */
object CritterRewardPriority {
    fun score(context: DecisionContext, critter: Critter): PriorityScore {
        val board = context.self.board
        var score = PriorityScore(base = 50)

        if (context.progress.currentCultivationRoundNumber?.let { it <= 2 } == true) {
            score = score.adjusted(
                if (critter == Critter.BEE) 20 else 0,
                "Early Cultivation prefers Bees"
            )
        }

        val bees = board.bees
        val worms = board.worms
        score = when {
            critter == Critter.WORM && bees >= worms + 2 ->
                score.adjusted(25, "Bees exceed Worms by at least two")
            critter == Critter.BEE && bees < worms + 2 ->
                score.adjusted(10, "Bee is the normal reward preference")
            else -> score
        }
        return score
    }
}
