package dugsolutions.leaf.main.gather

import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.main.domain.MainDomain

class GatherMainDomain(
    private val game: Game,
    private val gameTime: GameTime,
    private val gatherPlayerInfo: GatherPlayerInfo,
    private val gatherGroveInfo: GatherGroveInfo,
) {

    operator fun invoke(): MainDomain {
        return MainDomain(
            turn = gameTime.turn,
            players = game.players.map { gatherPlayerInfo(it) },
            groveInfo = gatherGroveInfo()
        )
    }
}
