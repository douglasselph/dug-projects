package dugsolutions.leaf.game.turn

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.player.Player

class PlayerOrder(
    private val chronicle: GameChronicle
) {

    operator fun invoke(players: List<Player>): List<Player> {
        return players
    }

}
