package dugsolutions.leaf.simulation.v35.analysis

import dugsolutions.leaf.v35.player.PlayerId

/**
 * Compact immutable research record for one completed game.
 *
 * This deliberately contains values only. It never retains Game, Player,
 * Chronicle, GameEntry, card, or die objects, so batch experiments can keep
 * thousands of summaries without keeping thousands of completed games alive.
 */
data class GameSummary(
    val mechanicalSeed: Long?,
    val strategySeed: Long?,
    val roundsCompleted: Int,
    val winnerIds: List<PlayerId>,
    val players: List<PlayerGameSummary>
) {
    init {
        require(roundsCompleted >= 0) { "Completed round count cannot be negative" }
        require(players.map { it.playerId }.distinct().size == players.size) {
            "Game summary cannot contain duplicate player IDs"
        }
        require(winnerIds.all { winner -> players.any { it.playerId == winner } }) {
            "Every winner must be present in the player summaries"
        }
    }
}

/** Compact end-state and Chronicle-derived metrics for one player. */
data class PlayerGameSummary(
    /** Zero-based physical seat in this game. */
    val seat: Int,
    val playerId: PlayerId,
    val won: Boolean,
    /** Shared winners split one win equally; non-winners receive zero. */
    val winShare: Double,
    val existingVp: Int,
    val plantVp: Int,
    val unplayedWispVp: Int,
    val totalVp: Int,
    val battleStrikeVp: Int,
    val woundsTaken: Int,
    /** Wisps obtained specifically through recorded Roll Rewards. */
    val rollRewardWispsGained: Int,
    /** Normal Wisp Support Actions plus immediate-play Wisp Roll Rewards. */
    val wispsPlayed: Int,
    val finalWispCount: Int,
    val finalPlantCount: Int,
    val finalPlantPrintedCost: Int,
    val finalDiceCount: Int,
    val finalDicePower: Int
) {
    init {
        require(seat >= 0) { "Seat cannot be negative: $seat" }
        require(winShare in 0.0..1.0) { "Invalid win share: $winShare" }
        require(battleStrikeVp >= 0)
        require(woundsTaken >= 0)
        require(rollRewardWispsGained >= 0)
        require(wispsPlayed >= 0)
        require(finalWispCount >= 0)
        require(finalPlantCount >= 0)
        require(finalPlantPrintedCost >= 0)
        require(finalDiceCount >= 0)
        require(finalDicePower >= 0)
    }
}
