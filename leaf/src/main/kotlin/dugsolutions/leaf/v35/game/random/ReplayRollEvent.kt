package dugsolutions.leaf.v35.game.random

/** Game-local, value-only evidence for a C2/C3-covered die roll. */
data class ReplayRollEvent(
    val ordinal: Int,
    val round: Int,
    val player: Int,
    val dieId: Long,
    val occurrence: Int,
    val sides: Int,
    val face: Int,
    /** Available only when the shared-percentile mode is active. */
    val percentile: Double?
) {
    val key: ReplayRollKey get() = ReplayRollKey(round, player, dieId, occurrence)
}

data class ReplayRollKey(val round: Int, val player: Int, val dieId: Long, val occurrence: Int)
