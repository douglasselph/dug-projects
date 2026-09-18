package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.strategy.planned.TargetCard
import dugsolutions.leaf.v35.game.GameRoundSetup

/**
 * Reproducible configuration for one focused-card experiment.
 *
 * [gamesPerSeat] is deliberately per seat. A 4-player experiment with
 * gamesPerSeat=1_000 therefore runs 4_000 games and gives the focused strategy
 * exactly the same number of starts in every seat.
 */
data class CardFocusExperimentSpec(
    val targetCard: TargetCard,
    val targetCount: Int = 1,
    val numPlayers: Int = 4,
    val gamesPerSeat: Int,
    val baseSeed: Long = 1L,
    val strategyBaseSeed: Long = baseSeed,
    val roundSetup: GameRoundSetup = GameRoundSetup.standard()
) {
    init {
        require(targetCount > 0) { "Target card count must be positive: $targetCount" }
        require(numPlayers in 2..4) { "Card experiments require 2 to 4 players: $numPlayers" }
        require(gamesPerSeat > 0) { "gamesPerSeat must be positive: $gamesPerSeat" }
    }

    val totalGames: Int
        get() = Math.multiplyExact(numPlayers, gamesPerSeat)
}
