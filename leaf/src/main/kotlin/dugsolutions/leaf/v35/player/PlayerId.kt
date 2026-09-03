package dugsolutions.leaf.v35.player

/**
 * Game-local identity for one player.
 *
 * Player IDs should be assigned explicitly by game/simulation setup rather
 * than generated from application-global state.
 */
@JvmInline
value class PlayerId(
    val value: Int
)
