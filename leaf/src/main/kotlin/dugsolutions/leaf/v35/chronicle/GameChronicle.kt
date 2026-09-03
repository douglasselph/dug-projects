package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.Moment

/**
 * In-memory Chronicle for one game/simulation.
 *
 * Recording is synchronized so sequence allocation, Moment transformation,
 * and insertion happen atomically relative to other Chronicle operations.
 */
class GameChronicle : Chronicle {

    private val lock = Any()
    private val storedEntries = mutableListOf<GameEntry>()
    private var nextSequence = 1L

    override fun record(moment: Moment): GameEntry =
        synchronized(lock) {
            val entry = transform(
                sequence = nextSequence,
                moment = moment
            )

            storedEntries.add(entry)
            nextSequence++

            entry
        }

    override val entries: List<GameEntry>
        get() = synchronized(lock) {
            storedEntries.toList()
        }

    override fun entriesAfter(sequence: Long): List<GameEntry> {
        require(sequence >= 0) {
            "Sequence cannot be negative: $sequence"
        }

        return synchronized(lock) {
            storedEntries
                .filter { it.sequence > sequence }
                .toList()
        }
    }

    override fun clear() {
        synchronized(lock) {
            storedEntries.clear()
            nextSequence = 1L
        }
    }

    private fun transform(
        sequence: Long,
        moment: Moment
    ): GameEntry =
        when (moment) {
            is Moment.Marker ->
                GameEntry.Marker(
                    sequence = sequence,
                    message = moment.message
                )
        }
}
