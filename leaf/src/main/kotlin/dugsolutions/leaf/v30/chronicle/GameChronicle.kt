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
                cardId = moment.card?.id,
                cardName = moment.card?.name,
                actualCount = moment.actualCount
            )
            is Moment.LoadingWarning -> GameEntry.LoadingWarning(
                sequence = sequence,
                time = time,
                name = moment.name,
                title = moment.title,
                reason = moment.reason
            )
            is Moment.RoundRevealed -> GameEntry.RoundRevealed(
                sequence = sequence,
                time = time,
                roundCardId = moment.card.id,
                roundCardName = moment.card.name,
                roundCardTitle = moment.card.title,
                roundCardType = moment.card.cardType
            )
            is Moment.DiceRolled -> GameEntry.DiceRolled(
                sequence = sequence,
                time = time,
                playerId = moment.player.id,
                dice = moment.player.diceHand.copy
            )
            is Moment.Reward -> GameEntry.Reward(
                sequence = sequence,
                time = time,
                playerId = moment.player.id,
                die = moment.die.copy,
                critter = moment.critter,
                wispCardId = moment.wispCard?.id,
                wispCardName = moment.wispCard?.name,
                wispCardTitle = moment.wispCard?.title,
                token = moment.token
            )
            is Moment.MainAction -> GameEntry.MainAction(
                sequence = sequence,
                time = time,
                playerId = moment.player.id,
                action = moment.action,
                detail = moment.detail,
                die = moment.die?.copy,
                token = moment.token,
                cardId = moment.card?.id,
                cardName = moment.card?.name,
                wispCardId = moment.wispCard?.id,
                wispCardName = moment.wispCard?.name
            )
            is Moment.GameCardEffect -> GameEntry.GameCardEffect(
                sequence = sequence,
                time = time,
                playerId = moment.player.id,
                cardId = moment.card.id,
                cardName = moment.card.name,
                effect = moment.effect,
                detail = moment.detail,
                dice = moment.dice?.copy.orEmpty(),
                token = moment.token,
                critter = moment.critter
            )
            is Moment.VpAward -> GameEntry.VpAward(
                sequence = sequence,
                time = time,
                playerId = moment.player.id,
                row = moment.row,
                amount = moment.amount
            )
            is Moment.WoundCard -> GameEntry.WoundCard(
                sequence = sequence,
                time = time,
                playerId = moment.player.id,
                cardId = moment.card.card.id,
                cardName = moment.card.card.name,
                facingBefore = moment.card.facing,
                wasFlipped = moment.wasFlipped,
                wasLost = moment.wasLost
            )
        }
    }
}
