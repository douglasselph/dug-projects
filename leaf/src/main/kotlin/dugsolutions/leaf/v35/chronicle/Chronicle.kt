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
     * When called inside [scoped], the entry is buffered as a child of the
     * current scope and receives the corresponding hierarchy depth.
     */
    fun record(moment: Moment): GameEntry

    /**
     * Executes [block] as one logical Chronicle scope.
     *
     * Child Chronicle entries are buffered while [block] runs. If it succeeds,
     * [parent] is committed first, followed by the buffered children. If it
     * fails, the entire scope is discarded, including its reserved sequence
     * numbers. Scopes may nest; each nested level increases hierarchy depth by 1.
     */
    fun <T> scoped(parent: Moment, block: () -> T): T =
        scoped(parent = { parent }, block = block)

    /**
     * Deferred-parent variant of [scoped]. Use this when parent metadata is only
     * known after [block] completes (for example cleanup counts or Strike wounds).
     */
    fun <T> scoped(parent: () -> Moment, block: () -> T): T

    /** Defensive snapshot of all committed entries recorded so far. */
    val entries: List<GameEntry>

    /**
     * Returns committed entries whose sequence is strictly greater than [sequence].
     * Passing 0 returns all committed entries. In-progress scoped entries are not
     * visible until their outermost scope commits.
     */
    fun entriesAfter(sequence: Long): List<GameEntry>

    /** Removes all committed entries and resets the next sequence number to 1. */
    fun clear()
}
