package dugsolutions.leaf.v30.chronicle.domain

class GameEntryMessage {
    operator fun invoke(entry: GameEntry): String {
        return when (entry) {
            is GameEntry.Warning -> warning(entry)
            is GameEntry.LoadingWarning -> loadingWarning(entry)
        }
    }

    private fun warning(entry: GameEntry.Warning): String {
        return "WARNING round=${entry.time.round} player=${entry.playerId} type=${entry.type}" +
            entry.cardName?.let { " card=$it" }.orEmpty() +
            entry.actualCount?.let { " actualCount=$it" }.orEmpty()
    }

    private fun loadingWarning(entry: GameEntry.LoadingWarning): String {
        return "LOADING_WARNING name=${entry.name} title=${entry.title} reason=${entry.reason}"
    }
}
