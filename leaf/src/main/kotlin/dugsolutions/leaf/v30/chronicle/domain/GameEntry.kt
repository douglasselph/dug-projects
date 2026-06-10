package dugsolutions.leaf.v30.chronicle.domain

import dugsolutions.leaf.v30.cards.domain.GameCardID

sealed class GameEntry(
    open val sequence: Long,
    open val time: GameTimeSnapshot,
    open val kind: EntryKind,
    open val playerId: Int
) {
    data class Warning(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        override val playerId: Int,
        val type: WarningType,
        val cardId: GameCardID,
        val cardName: String
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.WARNING,
        playerId = playerId
    )
}
