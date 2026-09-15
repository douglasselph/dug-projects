package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.battle.*
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.battle.MechanicalBattleStrategy

class HumanBaselineBattleStrategy(
    private val delegate: BattleStrategy = MechanicalBattleStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine()
) : BattleStrategy {
    override fun chooseFirstMainAction(request: ChooseBattleFirstMainActionRequest): BattleMainAction {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseFirstMainAction(request)
        return scoreEngine.chooseValue(
            request.legalChoices.map { action ->
                ScoredChoice(action, BattleMainPriority.score(request.context, request.roundCard, action))
            }
        )
    }

    override fun chooseTurnAction(request: ChooseBattleTurnActionRequest): BattleTurnAction {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseTurnAction(request)
        val scored = request.legalChoices.map { choice ->
            val score = when (choice) {
                is BattleTurnAction.FinalMain ->
                    BattleMainPriority.score(request.context, request.roundCard, choice.action)
                        .adjusted(5, "Final Main action ends participation")
                is BattleTurnAction.Support -> BattleSupportPriority.score(request.context, choice.action)
            }
            ScoredChoice(choice, score)
        }
        return scoreEngine.chooseValue(scored)
    }

    override fun chooseDiePlacement(request: ChooseBattleDiePlacementRequest): StrikeRow {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDiePlacement(request)
        return scoreEngine.chooseValue(
            request.legalRows.map { row ->
                ScoredChoice(row, BattlePlacementPriority.score(request.context, row, request.die.value))
            }
        )
    }
}
