package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.Randomizer

/**
 * Targeting Rules
 * Players target based on chain order:
 * - Each player targets the player next lowest in the chain order
 * - The last player in chain order targets the highest player in chain order
 * 
 * Note: The incoming orderedPlayers list is already sorted with tiebreakers applied:
 * - First place ties are resolved by rerolling until a single lowest total is determined
 * - Other position ties are resolved by proximity (clockwise from highest-ranked player)
 * - Index 0 contains the highest-ranked player
 */
class HandleGetTarget {

    /**
     * @return The player that the current player will choose to target based on chain order rules.
     */
    operator fun invoke(player: Player, orderedPlayers: List<Player>): Player {
        if (orderedPlayers.size <= 1) {
            throw Exception("Expected an array greater than 1")
        }
        val currentIndex = orderedPlayers.indexOf(player)
        return if (currentIndex == orderedPlayers.size - 1) {
            // If current player is last, target the highest (first in list)
            orderedPlayers.first()
        } else {
            // Otherwise target the next player in sequence
            orderedPlayers[currentIndex + 1]
        }
    }
} 
