package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.Moment

/**
 * In-memory Chronicle for one game/simulation.
 *
 * Recording is synchronized so sequence allocation, Moment transformation,
 * and insertion happen atomically relative to other Chronicle operations.
 */
class GameChronicle : Chronicle {

    private val lock = Any()
    private val storedEntries = mutableListOf<GameEntry>()
    private var nextSequence = 1L

    override fun record(moment: Moment): GameEntry =
        synchronized(lock) {
            val entry = transform(
                sequence = nextSequence,
                moment = moment
            )

            storedEntries.add(entry)
            nextSequence++

            entry
        }

    override val entries: List<GameEntry>
        get() = synchronized(lock) {
            storedEntries.toList()
        }

    override fun entriesAfter(sequence: Long): List<GameEntry> {
        require(sequence >= 0) {
            "Sequence cannot be negative: $sequence"
        }

        return synchronized(lock) {
            storedEntries
                .filter { it.sequence > sequence }
                .toList()
        }
    }

    override fun clear() {
        synchronized(lock) {
            storedEntries.clear()
            nextSequence = 1L
        }
    }

    private fun transform(sequence: Long, moment: Moment): GameEntry =
        when (moment) {
            is Moment.Marker -> GameEntry.Marker(sequence, moment.message)
            is Moment.RoundRevealed -> GameEntry.RoundRevealed(
                sequence, moment.roundNumber, moment.cardName, moment.cardType,
                moment.firstEffect, moment.secondEffect
            )
            is Moment.RoundCompleted -> GameEntry.RoundCompleted(
                sequence, moment.roundNumber, moment.cardName, moment.cardType
            )
            is Moment.DieRolled -> GameEntry.DieRolled(
                sequence, moment.playerId, moment.sides, moment.value,
                moment.rewardPolicy, moment.reason
            )
            is Moment.RollReward -> GameEntry.RollReward(
                sequence, moment.playerId, moment.kind, moment.critter, moment.wispName
            )
            is Moment.OpeningDrawCompleted -> GameEntry.OpeningDrawCompleted(
                sequence, moment.phase, moment.playerId, moment.count
            )
            is Moment.MainAction -> GameEntry.MainAction(
                sequence, moment.playerId, moment.phase, moment.action,
                moment.actionNumber, moment.battleStage
            )
            is Moment.SupportAction -> GameEntry.SupportAction(
                sequence, moment.playerId, moment.phase, moment.action, moment.row
            )
            is Moment.EffectResolved -> GameEntry.EffectResolved(
                sequence, moment.playerId, moment.effect, moment.sourceKind,
                moment.sourceName, moment.phase
            )
            is Moment.BuyOrder -> GameEntry.BuyOrder(sequence, moment.order.toList())
            is Moment.Purchase -> GameEntry.Purchase(
                sequence, moment.playerId, moment.kind, moment.itemName,
                moment.cost, moment.paymentTotal
            )
            is Moment.Graft -> GameEntry.Graft(sequence, moment.playerId, moment.plantName)
            is Moment.BattleOrder -> GameEntry.BattleOrder(
                sequence, moment.order.toList(), moment.initialDiceCount
            )
            is Moment.StrikeResolved -> GameEntry.StrikeResolved(
                sequence,
                moment.row,
                moment.totals.toList(),
                moment.winnerIds.toList(),
                moment.woundedPlayerIds.toList(),
                moment.vpPerWinner
            )
            is Moment.Wound -> GameEntry.Wound(
                sequence, moment.playerId, moment.kind, moment.plantName
            )
            is Moment.Doom -> GameEntry.Doom(sequence, moment.dice.toList())
            is Moment.Refresh -> GameEntry.Refresh(sequence, moment.playerId)
            is Moment.Cleanup -> GameEntry.Cleanup(
                sequence, moment.playerId, moment.phase, moment.discardedDice,
                moment.returnedCritters, moment.refreshed
            )
            is Moment.Upgrade -> GameEntry.Upgrade(
                sequence, moment.playerId, moment.from, moment.to, moment.destination
            )
            is Moment.TrashDie -> GameEntry.TrashDie(
                sequence, moment.playerId, moment.sides, moment.destination
            )
            is Moment.FinalScore -> GameEntry.FinalScore(
                sequence, moment.playerId, moment.existingVp, moment.plantVp,
                moment.unplayedWispVp, moment.totalVp, moment.graftedPlantCount
            )
            is Moment.FinalWinners -> GameEntry.FinalWinners(
                sequence, moment.winnerIds.toList()
            )
            is Moment.GameCompleted -> GameEntry.GameCompleted(
                sequence, moment.roundsCompleted
            )
        }
}
