package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.battle.BaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleStrategy
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleDiePlacementRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleFirstMainActionRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleTurnActionRequest

class ScriptedBattleStrategy(
    private val fallback: BattleStrategy = BaselineBattleStrategy()
) : BattleStrategy {
    private val firstMain =
        DecisionScript<ChooseBattleFirstMainActionRequest, BattleMainAction>(
            "Battle first Main Actions"
        )
    private val turns =
        DecisionScript<ChooseBattleTurnActionRequest, BattleTurnAction>(
            "Battle Step-5 actions"
        )
    private val placements =
        DecisionScript<ChooseBattleDiePlacementRequest, StrikeRow>(
            "Battle die placements"
        )

    fun thenFirstMain(
        selector: (ChooseBattleFirstMainActionRequest) -> BattleMainAction
    ): ScriptedBattleStrategy = apply { firstMain.then(selector) }

    fun thenFirstMain(choice: BattleMainAction): ScriptedBattleStrategy =
        thenFirstMain { choice }

    fun thenTurn(
        selector: (ChooseBattleTurnActionRequest) -> BattleTurnAction
    ): ScriptedBattleStrategy = apply { turns.then(selector) }

    fun thenTurn(choice: BattleTurnAction): ScriptedBattleStrategy =
        thenTurn { choice }

    fun thenPlacement(
        selector: (ChooseBattleDiePlacementRequest) -> StrikeRow
    ): ScriptedBattleStrategy = apply { placements.then(selector) }

    fun thenPlacement(row: StrikeRow): ScriptedBattleStrategy =
        thenPlacement { row }

    override fun chooseFirstMainAction(
        request: ChooseBattleFirstMainActionRequest
    ): BattleMainAction {
        val chosen = firstMain.nextOrElse(request, fallback::chooseFirstMainAction)
        require(chosen in request.legalChoices) {
            "Scripted Battle first Main Action is not legal: $chosen; " +
                "legal=${request.legalChoices}"
        }
        return chosen
    }

    override fun chooseTurnAction(
        request: ChooseBattleTurnActionRequest
    ): BattleTurnAction {
        val chosen = turns.nextOrElse(request, fallback::chooseTurnAction)
        require(chosen in request.legalChoices) {
            "Scripted Battle turn action is not legal: $chosen; " +
                "legal=${request.legalChoices}"
        }
        return chosen
    }

    override fun chooseDiePlacement(
        request: ChooseBattleDiePlacementRequest
    ): StrikeRow {
        val chosen = placements.nextOrElse(request, fallback::chooseDiePlacement)
        require(chosen in request.legalRows) {
            "Scripted Battle die placement is not legal: $chosen; " +
                "legal=${request.legalRows}"
        }
        return chosen
    }

    fun assertExhausted() {
        firstMain.assertExhausted()
        turns.assertExhausted()
        placements.assertExhausted()
    }
}
