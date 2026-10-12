package dugsolutions.leaf.v35.player.decision.random

/**
 * Deterministic, separate strategy random streams for a single game.
 *
 * Mechanical events must continue to use GameConfig.seed and the Game's Randomizer.
 * These seeds are for strategy decisions only. Streams are isolated per player,
 * and scoring-range sampling does not advance the player's ordinary strategy RNG.
 *
 * This is call-order isolation between streams, NOT event-keyed mechanical replay.
 */
object StrategySeedStreams {
    enum class Purpose { DECISIONS, SCORE_RANGES }

    private const val PLAYER_STEP: Long = 0x5DEECE66DL
    private const val SCORE_SALT: Long = 0x5A17BEEFL

    /** Null preserves the legacy non-seeded behavior. */
    fun seed(baseSeed: Long?, playerIndex: Int, purpose: Purpose): Long? {
        require(playerIndex >= 0) { "Player index must be nonnegative: $playerIndex" }
        val playerSeed = baseSeed?.xor(PLAYER_STEP * (playerIndex + 1L)) ?: return null
        return when (purpose) {
            Purpose.DECISIONS -> playerSeed
            Purpose.SCORE_RANGES -> playerSeed xor SCORE_SALT
        }
    }
}
