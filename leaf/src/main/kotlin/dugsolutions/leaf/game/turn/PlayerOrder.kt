package dugsolutions.leaf.game.turn

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.player.Player

/**
 * Handles player ordering for both Cultivation and Battle phases
 */
class PlayerOrder(
    private val chronicle: GameChronicle
) {

    private var hadReroll = false

    /**
     * Determine Chain Order
     * At the start of each round, all players total the pip values of the dice they rolled.
     * Players are then ordered from lowest to highest total. This determines the Chain Order for
     * that round, which governs the order of card play and resolution effects.
     *
     * 🪙 Tie-Breaking
     * If two or more players are tied for the highest position, all players reroll their dice
     * until there is one clear winner for the highest.
     *
     * If players are tied otherwise in their pip total the player closest clockwise to the highest
     * ranking player is considered higher in the Chain Order.
     *
     * @return List of players in order (lowest to highest)
     */
    operator fun invoke(players: List<Player>): List<Player> {
        hadReroll = false
        // Establish there is but one highest ranking player
        val highestRankingPlayerIndex = ensureExactlyOneHighestRankingPlayer(players)

        // Create player positions with their pip totals and indices
        val positions = players.mapIndexed { index, player ->
            PlayerPosition(
                playerIndex = index,
                player = player,
                totalPips = player.pipTotal
            )
        }

        // Sort players by total pips (highest first)
        // For ties, use relative clockwise distance from the highest ranking player
        val sortedPositions = positions.sortedWith(
            compareByDescending<PlayerPosition> { it.totalPips }
                .thenBy {
                    getRelativeClockwiseDistance(
                        it.playerIndex,
                        highestRankingPlayerIndex,
                        players.size
                    )
                }
        )

        // Log the ordering event
        chronicle(GameChronicle.Moment.ORDERING(sortedPositions.map { it.player }, hadReroll))

        // Return the players in order
        return sortedPositions.map { it.player }
    }

    /**
     * Ensure there is but one player who is the highest ranking
     *
     * @return the index of the highest ranking player.
     */
    private fun ensureExactlyOneHighestRankingPlayer(players: List<Player>): Int {
        for (i in 0..10) {
            val positions = players.mapIndexed { index, player ->
                PlayerPosition(
                    playerIndex = index,
                    player = player,
                    totalPips = player.diceInHand.dice.sumOf { it.value }
                )
            }
            val sortedPositions = positions.sortedByDescending { it.totalPips }
            if (sortedPositions[0].totalPips == 0) {
                return 0
            }
            if (sortedPositions[0].totalPips == sortedPositions[1].totalPips) {
                players.forEach { player -> player.diceInHand.reroll() }
                hadReroll = true
            } else {
                return sortedPositions[0].playerIndex
            }
        }
        println("Could not resolve highest rolled player")
        return 0
    }

    /**
     * Calculate the clockwise distance from the highest ranking player  to the given player.
     * - Returns 0 for the First Player Token holder
     * - Returns 1, 2, 3, etc. in clockwise order from the First Player Token holder
     *
     * Lower values are considered "closer" in clockwise order.
     */
    private fun getRelativeClockwiseDistance(
        playerIndex: Int,
        highestRankingPlayerIndex: Int,
        numPlayers: Int
    ): Int {
        // Calculate the clockwise distance from tokenHolderIndex to playerIndex
        return (playerIndex - highestRankingPlayerIndex + numPlayers) % numPlayers
    }

    /**
     * Simple data class to hold player information for ordering
     */
    private data class PlayerPosition(
        val playerIndex: Int,
        val player: Player,
        val totalPips: Int
    )
} 