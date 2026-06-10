package dugsolutions.leaf.v30.chronicle

import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.GameTimeSnapshot
import dugsolutions.leaf.v30.chronicle.domain.Moment

class GameChronicle(
    private val currentRound: () -> Int = { 0 }
) : Chronicle {

    private val entries = mutableListOf<GameEntry>()
    private val lock = Any()
    private var lastNewEntriesIndex = 0
    private var nextSequence = 1L

    var hasNewEntry: (entry: GameEntry) -> Unit = {}

    override operator fun invoke(moment: Moment): GameEntry {
        val entry = transform(moment)
        synchronized(lock) {
            entries.add(entry)
        }
        hasNewEntry(entry)
        return entry
    }

    override fun getEntries(): List<GameEntry> {
        return synchronized(lock) { entries.toList() }
    }

    override fun getNewEntries(): List<GameEntry> {
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

    private fun transform(moment: Moment): GameEntry {
        val sequence = synchronized(lock) { nextSequence++ }
        val time = GameTimeSnapshot(round = currentRound())
        return when (moment) {
            is Moment.Warning -> GameEntry.Warning(
                sequence = sequence,
                time = time,
                playerId = moment.player.id,
                type = moment.type,
                cardId = moment.card.id,
                cardName = moment.card.name
            )
        }
    }
}
