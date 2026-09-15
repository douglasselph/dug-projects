package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.RowNeedHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter
import kotlin.math.roundToInt

object BattleSupportPriority {
    fun score(context: DecisionContext, action: BattleSupportAction): PriorityScore =
        when (action) {
            is BattleSupportAction.PlaceCritter -> scoreCritter(context, action)
            is BattleSupportAction.Shared -> scoreShared(context, action.action)
        }

    private fun scoreCritter(context: DecisionContext, action: BattleSupportAction.PlaceCritter): PriorityScore {
        val need = RowNeedHeuristics.calculate(context, action.row)
        val value = if (action.critter == Critter.BEE) context.self.board.beeValue else context.self.board.wormValue
        var score = PriorityScore(25 + need.needScore)
        if (!need.currentlyWinning && need.pointsToBecomeWinner in 1..value) {
            score = score.adjusted(25, "Critter changes the row to a win")
        }
        if (need.woundRisk && need.pointsToAvoidWound in 1..value) {
            score = score.adjusted(15, "Critter escapes Wound range")
        }
        return score
    }

    private fun scoreShared(context: DecisionContext, action: SupportAction): PriorityScore =
        when (action) {
            is SupportAction.UseWaterReroll -> {
                val row = findRow(context, action.die.index)
                val expected = DieValueHeuristics.expectedRerollGain(action.die.sides, action.die.value)
                val need = row?.let { RowNeedHeuristics.calculate(context, it).needScore } ?: 0
                PriorityScore(25 + (expected * 4).roundToInt() + need / 3)
            }
            SupportAction.UseWaterRefresh -> {
                val spent = context.self.board.creature.count { it.isFaceDown }
                PriorityScore(20 + spent * 15)
            }
            is SupportAction.UseMulch -> {
                val sides = action.token.sides?.value ?: 4
                val highestNeed = context.battle?.rows?.maxOfOrNull {
                    RowNeedHeuristics.calculate(context, it.row).needScore
                } ?: 0
                PriorityScore(35 + (DieValueHeuristics.expectedRoll(sides) * 3).roundToInt() + highestNeed / 4)
            }
            is SupportAction.UseWormFlip -> PriorityScore(35).adjusted(10, "Refreshing a spent Plant can create another useful action")
            is SupportAction.UseButterfly -> {
                val expected = DieValueHeuristics.expectedKeepBestRerollGain(action.die.sides, action.die.value)
                val row = findRow(context, action.die.index)
                val need = row?.let { RowNeedHeuristics.calculate(context, it).needScore } ?: 0
                PriorityScore(30 + (expected * 5).roundToInt() + need / 4)
            }
            is SupportAction.PlayWisp -> PriorityScore(25) // Step 7 supplies Wisp-specific play scoring
        }

    private fun findRow(context: DecisionContext, handIndex: Int) =
        context.battle?.rows?.firstOrNull { row ->
            row.forPlayer(context.self.id)?.dice?.any { it.handIndex == handIndex } == true
        }?.row
}
