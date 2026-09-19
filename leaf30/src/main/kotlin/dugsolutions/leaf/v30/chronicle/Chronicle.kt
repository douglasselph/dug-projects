package dugsolutions.leaf.v30.chronicle

import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.Moment

interface Chronicle {
    operator fun invoke(moment: Moment): GameEntry
    fun getEntries(): List<GameEntry>
    fun getNewEntries(): List<GameEntry>
    fun clear()
}
