package dugsolutions.leaf.v30.chronicle.decision

interface DecisionCountLog {
    operator fun invoke(count: DecisionCount): DecisionCountEntry
    fun getEntries(): List<DecisionCountEntry>
    fun getNewEntries(): List<DecisionCountEntry>
    fun clear()
}
