package dugsolutions.leaf.v35.player.decision.mechanical.battle

import dugsolutions.leaf.v35.player.decision.battle.*

/**
 * Deliberately simple deterministic Battle policy.
 *
 * It prefers Draw for Main Actions when legal. During Step 5 it finishes as
 * soon as possible rather than automatically spending Support resources.
 */
class MechanicalBattleStrategy : BattleStrategy {

    override fun chooseFirstMainAction(
        request: ChooseBattleFirstMainActionRequest
    ): BattleMainAction =
        request.legalChoices.firstOrNull {
            it == BattleMainAction.Draw
        } ?: request.legalChoices.first()

    override fun chooseTurnAction(
        request: ChooseBattleTurnActionRequest
    ): BattleTurnAction {
        val finalChoices =
            request.legalChoices.filterIsInstance<BattleTurnAction.FinalMain>()

        return finalChoices.firstOrNull {
            it.action == BattleMainAction.Draw
        } ?: finalChoices.firstOrNull()
            ?: request.legalChoices.first()
    }

    override fun chooseDiePlacement(
        request: ChooseBattleDiePlacementRequest
    ) = request.legalRows.first()
}
