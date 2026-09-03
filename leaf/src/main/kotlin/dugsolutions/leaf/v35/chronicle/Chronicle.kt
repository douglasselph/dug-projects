package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.Moment

/**
 * Records the history of one game/simulation.
 *
 * A Chronicle is game-scoped, not application-scoped. All collaborators
 * belonging to the same simulation should receive the same Chronicle instance.
 */
interface Chronicle {

    /**
     * Records [moment] as an immutable [GameEntry].
     *
     * Sequence numbers begin at 1 and increase monotonically until [clear].
     */
    fun record(moment: Moment): GameEntry

    /**
     * Defensive snapshot of all entries recorded so far.
     */
    val entries: List<GameEntry>

    /**
     * Returns entries whose sequence is strictly greater than [sequence].
     * Passing 0 returns all entries.
     */
    fun entriesAfter(sequence: Long): List<GameEntry>

    /**
     * Removes all entries and resets the next sequence number to 1.
     */
    fun clear()
}
