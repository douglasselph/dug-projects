package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.common.RowNeedHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

object BattlePlacementPriority {
    fun score(context: DecisionContext, row: StrikeRow, dieValue: Int): PriorityScore {
        val need = RowNeedHeuristics.calculate(context, row)
        var score = PriorityScore(base = 30 + need.needScore)
        if (!need.available) return score.adjusted(-1000, "Row is unavailable")
        if (!need.currentlyWinning && dieValue >= need.pointsToBecomeWinner && need.pointsToBecomeWinner > 0) {
            score = score.adjusted(25, "Placement changes row to a win")
        }
        if (need.woundRisk && dieValue >= need.pointsToAvoidWound) {
            score = score.adjusted(15, "Placement escapes Wound range")
        }
        return score
    }
}
