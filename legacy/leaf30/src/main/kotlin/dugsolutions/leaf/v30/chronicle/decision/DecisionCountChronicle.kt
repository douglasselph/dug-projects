package dugsolutions.leaf.v30.chronicle.decision

import dugsolutions.leaf.v30.chronicle.domain.GameTimeSnapshot

class DecisionCountChronicle(
    private val currentRound: () -> Int = { 0 }
) : DecisionCountLog {

    private val entries = mutableListOf<DecisionCountEntry>()
    private val lock = Any()
    private var lastNewEntriesIndex = 0
    private var nextSequence = 1L

    var hasNewEntry: (entry: DecisionCountEntry) -> Unit = {}

    override operator fun invoke(count: DecisionCount): DecisionCountEntry {
        val entry = synchronized(lock) {
            DecisionCountEntry(
                sequence = nextSequence++,
                time = GameTimeSnapshot(round = currentRound()),
                playerId = count.playerId,
                type = count.type,
                count = count.count
            ).also { entries.add(it) }
        }
        hasNewEntry(entry)
        return entry
    }

    override fun getEntries(): List<DecisionCountEntry> {
        return synchronized(lock) { entries.toList() }
    }

    override fun getNewEntries(): List<DecisionCountEntry> {
        return synchronized(lock) {
            val newEntries = entries.subList(lastNewEntriesIndex, entries.size).toList()
            lastNewEntriesIndex = entries.size
            newEntries
        }
    }

    override fun clear() {
        synchronized(lock) {
            entries.clear()
            lastNewEntriesIndex = 0
            nextSequence = 1L
        }
    }
}
