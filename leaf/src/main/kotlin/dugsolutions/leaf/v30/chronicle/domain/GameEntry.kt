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
        val cardId: GameCardID?,
        val cardName: String?,
        val actualCount: Int? = null
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.WARNING,
        playerId = playerId
    )

    data class LoadingWarning(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        val name: String,
        val title: String,
        val reason: String
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.LOADING_WARNING,
        playerId = 0
    )
}
