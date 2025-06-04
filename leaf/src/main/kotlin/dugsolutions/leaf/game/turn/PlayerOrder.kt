package dugsolutions.leaf.game.turn

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

/**
 * Handles player ordering for both Cultivation and Battle phases
 */
class PlayerOrder(
    private val chronicle: GameChronicle
) {

    companion object {
        private const val maxAttempts = 20
    }
    private var numberOfRerolls = 0

    /**
     * Determine Chain Order
     * At the start of each round, all players total the pip values of the dice they rolled.
     * Players are then ordered from lowest to highest total. This determines the Chain Order for
     * that round, which governs the order of card play and resolution effects.
     *
     * 🪙 Tie-Breaking
     * If two or more players are tied for the highest position, the tied players must choose one die
     * in which to reroll. After this this chain order is evaluated again. This process continues until
     * there is one player has has the undisputed highest value.
     *
     * If players are tied otherwise in their pip total the player closest clockwise to the highest
     * ranking player is considered higher in the Chain Order.
     *
     * @return List of players in order (highest to lowest)
     */
    operator fun invoke(players: List<Player>): List<Player> {
        numberOfRerolls = 0
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
        chronicle(Moment.ORDERING(sortedPositions.map { it.player }, numberOfRerolls))

        // Return the players in order
        return sortedPositions.map { it.player }
    }

    /**
     * Ensure there is but one player who is the highest ranking
     *
     * @return the index of the highest ranking player.
     */
    private fun ensureExactlyOneHighestRankingPlayer(players: List<Player>): Int {
        while (numberOfRerolls < maxAttempts) {
            val positions = players.mapIndexed { index, player ->
                PlayerPosition(
                    playerIndex = index,
                    player = player,
                    totalPips = player.diceInHand.dice.sumOf { it.value }
                )
            }
            val sortedPositions = positions.sortedByDescending { it.totalPips }
            
            // If no dice have been rolled yet, return the first player
            if (sortedPositions[0].totalPips == 0) {
                return 0
            }

            // Check if there's a tie for highest position
            val highestPips = sortedPositions[0].totalPips
            val tiedPlayers = sortedPositions.filter { it.totalPips == highestPips }
            
            if (tiedPlayers.size == 1) {
                // We have a clear winner
                return tiedPlayers[0].playerIndex
            }

            // Only the tied players reroll one die
            tiedPlayers.forEach { position ->
                position.player.decisionDirector.rerollOneDie()
            }
            numberOfRerolls++
        }
        throw IllegalStateException("Could not resolve highest ranking player after $maxAttempts attempts")
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
