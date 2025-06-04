package dugsolutions.leaf.chronicle.local

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.domain.AcquireCardEntry
import dugsolutions.leaf.chronicle.domain.AcquireDieEntry
import dugsolutions.leaf.chronicle.domain.AddToThornEntry
import dugsolutions.leaf.chronicle.domain.AddToTotalEntry
import dugsolutions.leaf.chronicle.domain.AdjustDieEntry
import dugsolutions.leaf.chronicle.domain.AdjustDieToMax
import dugsolutions.leaf.chronicle.domain.AdornEntry
import dugsolutions.leaf.chronicle.domain.ChronicleEntry
import dugsolutions.leaf.chronicle.domain.DeflectDamageEntry
import dugsolutions.leaf.chronicle.domain.DeliverDamageEntry
import dugsolutions.leaf.chronicle.domain.DiscardCardEntry
import dugsolutions.leaf.chronicle.domain.DiscardDieEntry
import dugsolutions.leaf.chronicle.domain.DrawCardEntry
import dugsolutions.leaf.chronicle.domain.DrawDieEntry
import dugsolutions.leaf.chronicle.domain.DrawnHandEntry
import dugsolutions.leaf.chronicle.domain.EventBattleTransition
import dugsolutions.leaf.chronicle.domain.EventTurn
import dugsolutions.leaf.chronicle.domain.Finished
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.chronicle.domain.OrderingEntry
import dugsolutions.leaf.chronicle.domain.PlayCardEntry
import dugsolutions.leaf.chronicle.domain.RerollEntry
import dugsolutions.leaf.chronicle.domain.RetainCardEntry
import dugsolutions.leaf.chronicle.domain.RetainDieEntry
import dugsolutions.leaf.chronicle.domain.ReuseCardEntry
import dugsolutions.leaf.chronicle.domain.ScoreInfo
import dugsolutions.leaf.chronicle.domain.TrashCardEntry
import dugsolutions.leaf.chronicle.domain.TrashDieEntry
import dugsolutions.leaf.chronicle.domain.TrashForEffect
import dugsolutions.leaf.chronicle.domain.UpgradeDieEntry
import dugsolutions.leaf.chronicle.report.ReportDamage
import dugsolutions.leaf.chronicle.report.ReportGameBrief
import dugsolutions.leaf.chronicle.report.ReportPlayer
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.domain.GameTime

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
    operator fun invoke(moment: Moment, gameTime: GameTime): ChronicleEntry {
        return when (moment) {

            is Moment.ACQUIRE_CARD ->
                AcquireCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cardId = moment.card.id,
                    cardName = moment.card.name,
                    paid = moment.paid.copy()
                )

            is Moment.ACQUIRE_DIE ->
                AcquireDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    dieSides = moment.die.sides,
                    paid = moment.paid.copy()
                )

            is Moment.ADJUST_DIE ->
                AdjustDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    die = moment.die.copy,
                    adjustment = moment.amount
                )

            is Moment.ADD_TO_THORN ->
                AddToThornEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    amount = moment.amount
                )

            is Moment.ADD_TO_TOTAL ->
                AddToTotalEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    amount = moment.amount
                )

            is Moment.ADORN ->
                AdornEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cardId = moment.cardId
                )

            is Moment.DELIVER_DAMAGE ->
                DeliverDamageEntry(
                    playerId = moment.attacker.id,
                    turn = gameTime.turn,
                    report = reportDamage(moment)
                )

            is Moment.DRAW_CARD -> {
                val card = cardManager.getCard(moment.cardId)
                DrawCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cardId = moment.cardId,
                    cardName = card?.name ?: "unknown"
                )
            }

            is Moment.DRAW_DIE ->
                DrawDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    dieSides = moment.die.sides
                )

            is Moment.DRAWN_HAND ->
                DrawnHandEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cards = moment.player.cardsInHand.mapNotNull { cardManager.getCard(it.id)?.name },
                    dice = DieValues(moment.player.diceInHand.copy)
                )

            is Moment.DEFLECT_DAMAGE ->
                DeflectDamageEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    amount = moment.amount
                )

            is Moment.DISCARD_CARD ->
                DiscardCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cardId = moment.cardId.id,
                    cardName = moment.cardId.name
                )

            is Moment.DISCARD_DIE ->
                DiscardDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    dieSides = moment.die.sides
                )

            is Moment.EVENT_TURN ->
                EventTurn(
                    gamePhase = gameTime.phase,
                    turn = gameTime.turn,
                    reports = moment.players.sortedBy { it.name }.map { reportPlayer(it) },
                    scores = moment.players.map { player -> ScoreInfo(player.score) }
                )

            is Moment.EVENT_BATTLE_TRANSITION ->
                EventBattleTransition(
                    turn = gameTime.turn + 1,
                    score = moment.player.score,
                    trashedSeedlings = moment.trashedSeedlings.mapNotNull { cardManager.getCard(it)?.name }
                )

            is Moment.FINISHED ->
                Finished(
                    turn = gameTime.turn,
                    reports = reportGameBrief(moment.result),
                    scores = moment.result.players.map { data ->
                        ScoreInfo(data.score)
                    }
                )

            is Moment.ORDERING -> with(moment) {
                OrderingEntry(
                    playerId = players.firstOrNull()?.id ?: 0,
                    turn = gameTime.turn,
                    playerOrder = players.map { it.id },
                    reports = if (numberOfRerolls > 0) players.sortedBy { it.name }.map { reportPlayer(it) } else emptyList(),
                    numberRerolls = numberOfRerolls
                )
            }

            is Moment.PLAY_CARD ->
                PlayCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cardId = moment.card.id,
                    cardName = moment.card.name
                )

            is Moment.REROLL ->
                RerollEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    dieSides = moment.die.sides,
                    newValue = moment.die.value
                )

            is Moment.RETAIN_CARD ->
                RetainCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cardId = moment.card.id,
                    cardName = moment.card.name
                )

            is Moment.RETAIN_DIE ->
                RetainDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    dieSides = moment.die.sides
                )

            is Moment.REUSE_CARD ->
                ReuseCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cardId = moment.card.id,
                    cardName = moment.card.name
                )

            is Moment.SET_TO_MAX ->
                AdjustDieToMax(
                    playerId = moment.player.id,
                    turn = gameTime.turn
                )

            is Moment.TRASH_CARD ->
                TrashCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    card = moment.card
                )

            is Moment.TRASH_DIE ->
                TrashDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    dieSides = moment.die.sides
                )

            is Moment.TRASH_FOR_EFFECT ->
                TrashForEffect(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    card = moment.card.name,
                    status = moment.status
                )

            is Moment.UPGRADE_DIE ->
                UpgradeDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    newSides = moment.die.sides
                )

        }
    }

}
