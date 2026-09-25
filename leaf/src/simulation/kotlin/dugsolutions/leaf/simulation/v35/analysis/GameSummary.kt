package dugsolutions.leaf.simulation.v35.analysis

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureSide

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
    /** Canonical value-only description of the final Plant Creature. */
    val plantCreatureSignature: PlantCreatureSignature,
    val finalDiceCount: Int,
    val finalDicePower: Int,
    /** Canonical count-by-size description of all final owned dice. */
    val ownedDiceSignature: OwnedDiceSignature
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


/**
 * Canonical final Plant Creature shape for compact comparison across games.
 *
 * Cards are sorted by logical grid position, side, and stable Plant name.
 * Facing is intentionally excluded: this signature describes development
 * shape/card composition rather than transient ready/spent state.
 */
data class PlantCreatureSignature(
    val cards: List<PlantCreatureCardSignature>
)

data class PlantCreatureCardSignature(
    val plantName: String,
    val side: CreatureSide,
    val x: Int,
    val y: Int
)

/** Canonical final owned-dice shape, independent of which owned zone holds a die. */
data class OwnedDiceSignature(
    val d4: Int,
    val d6: Int,
    val d8: Int,
    val d10: Int,
    val d12: Int,
    val d20: Int
) {
    init {
        require(listOf(d4, d6, d8, d10, d12, d20).all { it >= 0 }) {
            "Owned die counts cannot be negative"
        }
    }

    val totalDice: Int
        get() = d4 + d6 + d8 + d10 + d12 + d20

    val totalPower: Int
        get() = d4 * 4 + d6 * 6 + d8 * 8 + d10 * 10 + d12 * 12 + d20 * 20
}
