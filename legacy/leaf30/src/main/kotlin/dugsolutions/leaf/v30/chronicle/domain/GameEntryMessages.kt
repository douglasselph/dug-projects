package dugsolutions.leaf.v30.chronicle.domain

class GameEntryMessages(
    private val gameEntryMessage: GameEntryMessage = GameEntryMessage()
) {
    operator fun invoke(entries: List<GameEntry>): List<String> {
        return entries.map { gameEntryMessage(it) }
    }
}
