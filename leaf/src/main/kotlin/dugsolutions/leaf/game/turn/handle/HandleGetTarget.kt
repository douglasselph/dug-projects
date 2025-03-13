package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.Randomizer

/**
 * Targeting Rules
 * When playing a card that targets another player you may only select a player that has not
 * yet been targeted on the current round during the turn.
 */
class HandleGetTarget(
    private val randomizer: Randomizer
) {

    /**
     * @return The player that the current player will choose to target.
     */
    operator fun invoke(player: Player, players: List<Player>): Player? {
        val possible = players.filter { !it.wasHit && it != player }
        if (possible.isEmpty()) {
            return null
        }
        val choice = randomizer.nextInt(0, possible.size)
        return possible[choice]
    }
} 
