package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionBattleInsect

class DecisionBattleInsectBaseline(
    private val player: Player
): DecisionBattleInsect {

    override suspend fun invoke(opposingPlayers: List<DecisionBattleInsect.BattleLine>): DecisionBattleInsect.Result {
        TODO("Not yet implemented")
    }

}
