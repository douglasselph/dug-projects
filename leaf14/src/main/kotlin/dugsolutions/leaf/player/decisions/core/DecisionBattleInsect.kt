package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.DieValue

interface DecisionBattleInsect {

    data class BattleLine(
        val player: Player,
        val line: List<DieValue>
    )

    data class ApplyToDie(
        val die: DieValue,
        val insects: List<Token>
    )

    data class Result(
        val adjustments: List<ApplyToDie>
    )

    suspend operator fun invoke(opposingPlayers: List<BattleLine>): Result

}
