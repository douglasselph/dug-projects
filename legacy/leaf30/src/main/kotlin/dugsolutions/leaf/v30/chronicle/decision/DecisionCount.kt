package dugsolutions.leaf.v30.chronicle.decision

data class DecisionCount(
    val playerId: Int,
    val type: DecisionCountType,
    val count: Int
) {
    init {
        require(count >= 0) { "Decision count must not be negative: $count" }
    }
}
