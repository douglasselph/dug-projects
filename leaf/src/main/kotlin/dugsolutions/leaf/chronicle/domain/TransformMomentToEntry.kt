package dugsolutions.leaf.chronicle.domain

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.report.ReportDamage
import dugsolutions.leaf.chronicle.report.ReportGameBrief
import dugsolutions.leaf.chronicle.report.ReportPlayer
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.domain.GameTurn

class TransformMomentToEntry(
    private val cardManager: CardManager,
    private val reportPlayer: ReportPlayer,
    private val reportDamage: ReportDamage,
    private val reportGameBrief: ReportGameBrief
) {

    /**
     * Transforms a game moment into its corresponding chronicle entry.
     * This function handles the conversion from the external representation (Moment)
     * to the internal storage format (ChronicleEntry).
     */
    operator fun invoke(moment: GameChronicle.Moment, gameTurn: GameTurn): ChronicleEntry {
        return when (moment) {

            is GameChronicle.Moment.ACQUIRE_CARD ->
                AcquireCardEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    cardId = moment.card.id,
                    cardName = moment.card.name,
                    paid = moment.paid.copy()
                )

            is GameChronicle.Moment.ACQUIRE_DIE ->
                AcquireDieEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    dieSides = moment.die.sides,
                    paid = moment.paid.copy()
                )

            is GameChronicle.Moment.ADJUST_DIE ->
                AdjustDieEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    dieSides = 0,
                    adjustment = moment.amount
                )

            is GameChronicle.Moment.ADD_TO_THORN ->
                AddToThornEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    amount = moment.amount
                )

            is GameChronicle.Moment.ADD_TO_TOTAL ->
                AddToTotalEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    amount = moment.amount
                )

            is GameChronicle.Moment.DELIVER_DAMAGE ->
                DeliverDamageEntry(
                    playerId = 0,
                    turn = gameTurn.turn,
                    report = reportDamage(moment)
                )

            is GameChronicle.Moment.DRAW_CARD -> {
                val card = cardManager.getCard(moment.cardId)
                DrawCardEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    cardId = moment.cardId,
                    cardName = card?.name ?: "unknown"
                )
            }

            is GameChronicle.Moment.DRAW_DIE ->
                DrawDieEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    dieSides = moment.die.sides
                )

            is GameChronicle.Moment.DRAW_HAND ->
                DrawHandEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    cards = moment.player.cardsInHand.map { it.id },
                    dice = DieValues(moment.player.diceInHand.copy)
                )

            is GameChronicle.Moment.DEFLECT_DAMAGE ->
                DeflectDamageEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    amount = moment.amount
                )

            is GameChronicle.Moment.DISCARD_CARD ->
                DiscardCardEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    cardId = moment.cardId.id,
                    cardName = moment.cardId.name
                )

            is GameChronicle.Moment.DISCARD_DIE ->
                DiscardDieEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    dieSides = moment.die.sides
                )

            is GameChronicle.Moment.EVENT_TURN ->
                EventTurn(
                    turn = gameTurn.turn,
                    reports = moment.players.sortedBy { it.name }.map { reportPlayer(it) },
                    scores = moment.players.map { player -> ScoreInfo(player.score) }
                )

            is GameChronicle.Moment.EVENT_BATTLE ->
                EventBattle(
                    turn = gameTurn.turn + 1,
                    scores = moment.result.players.map { data ->
                        ScoreInfo(data.player.score)
                    }
                )

            is GameChronicle.Moment.FINISHED ->
                Finished(
                    turn = gameTurn.turn,
                    reports = reportGameBrief(moment.result),
                    scores = moment.result.players.map { data ->
                        ScoreInfo(data.score)
                    }
                )

            is GameChronicle.Moment.ORDERING -> with(moment) {
                OrderingEntry(
                    playerId = players.firstOrNull()?.id ?: 0, // Using first player as the actor
                    turn = gameTurn.turn,
                    playerIdOrder = players.map { it.id },
                    reports = if (hadReroll) players.sortedBy { it.name }.map { reportPlayer(it) } else emptyList()
                )
            }

            is GameChronicle.Moment.PLAY_CARD ->
                PlayCardEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    cardId = moment.card.id,
                    cardName = moment.card.name
                )

            is GameChronicle.Moment.REROLL ->
                RerollEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    dieSides = moment.die.sides
                )

            is GameChronicle.Moment.RETAIN_CARD ->
                RetainCardEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    cardId = moment.card.id,
                    cardName = moment.card.name
                )

            is GameChronicle.Moment.RETAIN_DIE ->
                RetainDieEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    dieSides = moment.die.sides
                )

            is GameChronicle.Moment.REUSE_CARD ->
                ReuseCardEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    cardId = moment.card.id,
                    cardName = moment.card.name
                )

            is GameChronicle.Moment.SET_TO_MAX ->
                AdjustDieToMax(
                    playerId = moment.player.id,
                    turn = gameTurn.turn
                )

            is GameChronicle.Moment.TRASH_CARD ->
                TrashCardEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    cardId = moment.cardId
                )

            is GameChronicle.Moment.TRASH_DIE ->
                TrashDieEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    dieSides = moment.die.sides
                )

            is GameChronicle.Moment.UPGRADE_DIE ->
                UpgradeDieEntry(
                    playerId = moment.player.id,
                    turn = gameTurn.turn,
                    dieSides = moment.die.sides
                )

        }
    }

}
