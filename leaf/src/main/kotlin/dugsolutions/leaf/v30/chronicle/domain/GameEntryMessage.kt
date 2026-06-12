package dugsolutions.leaf.v30.chronicle.domain

class GameEntryMessage {
    operator fun invoke(entry: GameEntry): String {
        return when (entry) {
            is GameEntry.Warning -> warning(entry)
            is GameEntry.LoadingWarning -> loadingWarning(entry)
            is GameEntry.RoundRevealed -> roundRevealed(entry)
            is GameEntry.DiceRolled -> diceRolled(entry)
            is GameEntry.Reward -> reward(entry)
            is GameEntry.MainAction -> mainAction(entry)
            is GameEntry.GameCardEffect -> gameCardEffect(entry)
            is GameEntry.VpAward -> vpAward(entry)
            is GameEntry.WoundCard -> woundCard(entry)
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

    private fun roundRevealed(entry: GameEntry.RoundRevealed): String {
        return "Round ${entry.time.round}: revealed ${entry.roundCardType} round card ${entry.roundCardName} (${entry.roundCardTitle})."
    }

    private fun diceRolled(entry: GameEntry.DiceRolled): String {
        return "Round ${entry.time.round}: player ${entry.playerId} rolled dice ${entry.dice.joinToString { "D${it.sides}=${it.value}" }}."
    }

    private fun reward(entry: GameEntry.Reward): String {
        val received = when {
            entry.critter != null -> "received ${entry.critter}"
            entry.wispCardName != null -> "received wisp ${entry.wispCardName}" +
                entry.wispCardTitle?.let { " ($it)" }.orEmpty()
            entry.token != null -> "received token ${entry.token}"
            else -> "received no reward"
        }
        return "Round ${entry.time.round}: player ${entry.playerId} rolled D${entry.die.sides}=${entry.die.value} and $received."
    }

    private fun mainAction(entry: GameEntry.MainAction): String {
        return "Round ${entry.time.round}: player ${entry.playerId} performed ${entry.action}: ${entry.detail}" +
            entry.die?.let { " die=D${it.sides}=${it.value}" }.orEmpty() +
            entry.token?.let { " token=$it" }.orEmpty() +
            entry.cardName?.let { " card=$it" }.orEmpty() +
            entry.wispCardName?.let { " wisp=$it" }.orEmpty() +
            "."
    }

    private fun gameCardEffect(entry: GameEntry.GameCardEffect): String {
        return "Round ${entry.time.round}: player ${entry.playerId} resolved ${entry.effect} from ${entry.cardName}: ${entry.detail}" +
            (if (entry.dice.isNotEmpty()) " dice=${entry.dice.joinToString { "D${it.sides}=${it.value}" }}" else "") +
            entry.token?.let { " token=$it" }.orEmpty() +
            entry.critter?.let { " critter=$it" }.orEmpty() +
            "."
    }

    private fun vpAward(entry: GameEntry.VpAward): String {
        return "Round ${entry.time.round}: player ${entry.playerId} gained ${entry.amount} VP on ${entry.row}."
    }

    private fun woundCard(entry: GameEntry.WoundCard): String {
        val action = if (entry.wasFlipped) "flipped" else "lost"
        return "Round ${entry.time.round}: player ${entry.playerId} was wounded on card ${entry.cardName}: the card was $action."
    }
}
