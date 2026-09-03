package dugsolutions.leaf.v35.chronicle.domain

/**
 * Immutable recorded history for one game/simulation.
 *
 * GameEntry variants should contain only scalar values or immutable value
 * snapshots. They must never retain mutable gameplay objects.
 */
sealed interface GameEntry {

    val sequence: Long

    data class Marker(
        override val sequence: Long,
        val message: String
    ) : GameEntry
}
