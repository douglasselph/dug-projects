package dugsolutions.leaf.v30.battle

import dugsolutions.leaf.v30.battle.domain.Result
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.player.Player

class BattleAwardWinners(
    private val chronicle: Chronicle = GameChronicle()
) {

    private companion object {
        const val WINNER_VP = 2
        const val WOUNDED_BONUS_VP = 1
    }

    operator fun invoke(
        players: List<Player>,
        result: Result
    ) {
        val playersById = players.associateBy { it.id }
        result.rows.keys.forEach { row ->
            val rowResult = result[row]
            val vpAward = WINNER_VP + rowResult.wounded.size * WOUNDED_BONUS_VP
            rowResult.winners.forEach { playerId ->
                playersById[playerId]?.let { player ->
                    player.addVp(vpAward)
                    chronicle(
                        Moment.VpAward(
                            player = player,
                            row = row,
                            amount = vpAward
                        )
                    )
                }
            }
            rowResult.wounded.forEach { playerId ->
                playersById[playerId]?.flipItOrSnipIt()
            }
        }
    }
}
