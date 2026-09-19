package dugsolutions.leaf.v30.chronicle.decision

import dugsolutions.leaf.v30.chronicle.domain.GameTimeSnapshot

data class DecisionCountEntry(
    val sequence: Long,
    val time: GameTimeSnapshot,
    val playerId: Int,
    val type: DecisionCountType,
    val count: Int
)
