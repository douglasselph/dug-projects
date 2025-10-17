package dugsolutions.leaf.chronicle

import dugsolutions.leaf.chronicle.domain.ChronicleEntry
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.chronicle.local.TransformMomentToEntry
import dugsolutions.leaf.game.domain.GameTime

class GameChronicle(
    private val gameTime: GameTime,
    private val transformMomentToEntry: TransformMomentToEntry
) {

    // Cache to store all chronicle entries
    private val entries = mutableListOf<ChronicleEntry>()
    private var lastNewEntriesIndex = 0
    private val lock = Any()

    var hasNewEntry: (entry: ChronicleEntry) -> Unit = {}

    /**
     * Records a game moment by transforming it into a chronicle entry and storing it.
     */
    operator fun invoke(moment: Moment) {
        // Transform the moment into a chronicle entry
        val entry = transformMomentToEntry(moment, gameTime)
        synchronized(lock) {
            // Store the entry in the cache
            entries.add(entry)
            hasNewEntry(entry)
        }
    }

    /**
     * Returns all recorded chronicle entries.
     */
    fun getEntries(): List<ChronicleEntry> = synchronized(lock) {
        entries.toList()
    }

    /**
     * Returns only the new entries since the last call to getNewEntries().
     * The first call returns all entries.
     */
    fun getNewEntries(): List<ChronicleEntry> = synchronized(lock) {
        val newEntries = entries.subList(lastNewEntriesIndex, entries.size)
        lastNewEntriesIndex = entries.size
        return newEntries.toList()
    }

    /**
     * Clears all recorded entries.
     */
    fun clear() = synchronized(lock) {
        entries.clear()
        lastNewEntriesIndex = 0
    }

    /**
     * Compute total time taken thus far
     */
    val timeTaken: Int
        get() {
            return entries.sumOf { it.timeTaken }
        }
}
