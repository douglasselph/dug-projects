package dugsolutions.leaf.v14.chronicle.local

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.chronicle.domain.AcquireCardEntry
import dugsolutions.leaf.v14.chronicle.domain.AcquireDieEntry
import dugsolutions.leaf.v14.chronicle.domain.AcquireNone
import dugsolutions.leaf.v14.chronicle.domain.AddToThornEntry
import dugsolutions.leaf.v14.chronicle.domain.AddToTotalEntry
import dugsolutions.leaf.v14.chronicle.domain.AdjustDieEntry
import dugsolutions.leaf.v14.chronicle.domain.AdjustDieToMax
import dugsolutions.leaf.v14.chronicle.domain.AdornEntry
import dugsolutions.leaf.v14.chronicle.domain.ChronicleEntry
import dugsolutions.leaf.v14.chronicle.domain.CleanupEntry
import dugsolutions.leaf.v14.chronicle.domain.DeflectDamageEntry
import dugsolutions.leaf.v14.chronicle.domain.DeliverDamageEntry
import dugsolutions.leaf.v14.chronicle.domain.DiscardCardEntry
import dugsolutions.leaf.v14.chronicle.domain.DiscardDieEntry
import dugsolutions.leaf.v14.chronicle.domain.DrawCardEntry
import dugsolutions.leaf.v14.chronicle.domain.DrawDieEntry
import dugsolutions.leaf.v14.chronicle.domain.DrawnHandEntry
import dugsolutions.leaf.v14.chronicle.domain.EventBattleTransition
import dugsolutions.leaf.v14.chronicle.domain.EventTurn
import dugsolutions.leaf.v14.chronicle.domain.Finished
import dugsolutions.leaf.v14.chronicle.domain.GainD20Entry
import dugsolutions.leaf.v14.chronicle.domain.InfoEntry
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.chronicle.domain.NutrientReward
import dugsolutions.leaf.v14.chronicle.domain.OrderingEntry
import dugsolutions.leaf.v14.chronicle.domain.PlayCardEntry
import dugsolutions.leaf.v14.chronicle.domain.ReplayVineEntry
import dugsolutions.leaf.v14.chronicle.domain.ReportEntry
import dugsolutions.leaf.v14.chronicle.domain.ReportHand
import dugsolutions.leaf.v14.chronicle.domain.RerollEntry
import dugsolutions.leaf.v14.chronicle.domain.RetainCardEntry
import dugsolutions.leaf.v14.chronicle.domain.RetainDieEntry
import dugsolutions.leaf.v14.chronicle.domain.ReuseCardEntry
import dugsolutions.leaf.v14.chronicle.domain.ReuseDieEntry
import dugsolutions.leaf.v14.chronicle.domain.ScoreInfo
import dugsolutions.leaf.v14.chronicle.domain.TimeTaken
import dugsolutions.leaf.v14.chronicle.domain.TrashCardEntry
import dugsolutions.leaf.v14.chronicle.domain.TrashDieEntry
import dugsolutions.leaf.v14.chronicle.domain.TrashForEffect
import dugsolutions.leaf.v14.chronicle.domain.UpgradeDieEntry
import dugsolutions.leaf.v14.chronicle.domain.UseFlowers
import dugsolutions.leaf.v14.chronicle.domain.UseOpponentCardEntry
import dugsolutions.leaf.v14.chronicle.domain.UseOpponentDieEntry
import dugsolutions.leaf.v14.chronicle.report.ReportDamage
import dugsolutions.leaf.v14.chronicle.report.ReportGameBrief
import dugsolutions.leaf.v14.chronicle.report.ReportPlayer
import dugsolutions.leaf.v14.game.domain.GameTime
import dugsolutions.leaf.v14.player.domain.AppliedEffect

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

            is Moment.ACQUIRE_NONE ->
                AcquireNone(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    diceTotal = moment.player.diceTotal,
                    pipModifier = moment.player.pipModifier,
                    effects = moment.player.delayedEffectList.toString()
                )

            is Moment.ADJUST_DIE ->
                AdjustDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.ADJUST_DIE,
                    die = moment.die.copy,
                    adjustment = moment.amount,
                    dice = moment.player.diceInHand.values()
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
                    timeTaken = TimeTaken.ADD_TO_TOTAL,
                    amount = moment.amount,
                    pips = moment.player.pipTotal
                )

            is Moment.ADORN ->
                AdornEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.ADORN,
                    drawCardId = moment.drawCardId,
                    flowerCardId = moment.flowerCardId
                )

            is Moment.CLEANUP ->
                CleanupEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.CLEANUP_BASE + (moment.numReused + moment.numRetained) * TimeTaken.CLEANUP_PER_ITEM,
                    numReused = moment.numReused,
                    numRetained = moment.numRetained
                )

            is Moment.DEFLECT_DAMAGE ->
                DeflectDamageEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.DEFLECT_DAMAGE,
                    amount = moment.amount
                )

            is Moment.DELIVER_DAMAGE ->
                DeliverDamageEntry(
                    playerId = moment.defender.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.DELIVER_DAMAGE,
                    report = reportDamage(moment)
                )

            is Moment.DISCARD_CARD ->
                DiscardCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.DISCARD_CARD,
                    cardId = moment.cardId.id,
                    cardName = moment.cardId.name
                )

            is Moment.DISCARD_DIE ->
                DiscardDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.DISCARD_DIE,
                    die = moment.die.copy
                )

            is Moment.DRAW_CARD -> {
                val card = cardManager.getCard(moment.cardId)
                DrawCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.DRAW_CARD + if (moment.hadReshuffle) TimeTaken.RESHUFFLE else 0,
                    cardId = moment.cardId,
                    cardName = card?.name ?: "unknown"
                )
            }

            is Moment.DRAW_DIE ->
                DrawDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.DRAW_DIE + if (moment.hadReshuffle) TimeTaken.RESHUFFLE else 0,
                    dieSides = moment.die.sides
                )

            is Moment.DRAWN_HAND ->
                DrawnHandEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cards = moment.player.cardsInHand.mapNotNull { cardManager.getCard(it.id)?.name },
                    dice = moment.player.diceInHand.values()
                )

            is Moment.EVENT_TURN ->
                EventTurn(
                    gamePhase = gameTime.phase,
                    turn = gameTime.turn,
                    reports = moment.players.sortedBy { it.name }.map { reportPlayer(it) },
                    scores = moment.players.map { player -> ScoreInfo(player.score) },
                    totalTimeTakenSeconds = moment.totalTimeTakenSeconds
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

            is Moment.GAIN_D20 ->
                GainD20Entry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                )

            is Moment.INFO ->
                InfoEntry(
                    turn = gameTime.turn,
                    message = moment.message
                )

            is Moment.NUTRIENT_REWARD ->
                NutrientReward(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.NUTRIENT_REWARD,
                    hadNutrients = moment.nutrients,
                    sidesGained = moment.gained.value
                )

            is Moment.ORDERING -> with(moment) {
                OrderingEntry(
                    playerId = players.firstOrNull()?.id ?: 0,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.ORDERING_REROLL * numberOfRerolls,
                    playerOrder = players.map { it.id },
                    reports = if (numberOfRerolls > 0) players.sortedBy { it.name }
                        .map { reportPlayer(it) } else emptyList(),
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
                    timeTaken = TimeTaken.REROLL,
                    dieSides = moment.die.sides,
                    before = moment.beforeValue,
                    newValue = moment.die.value,
                    dice = moment.player.diceInHand.values()
                )

            is Moment.RETAIN_CARD ->
                RetainCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.RETAIN_CARD,
                    cardId = moment.card.id,
                    cardName = moment.card.name
                )

            is Moment.RETAIN_DIE ->
                RetainDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.RETAIN_DIE,
                    dieSides = moment.die.sides
                )

            is Moment.REPLAY_VINE ->
                ReplayVineEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.REPLAY_VINE,
                    vineId = moment.selectedVine.id,
                    vineName = moment.selectedVine.name
                )

            is Moment.REPORT ->
                ReportEntry(
                    turn = gameTime.turn,
                    line = moment.line
                )

            is Moment.REPORT_HAND ->
                ReportHand(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    cards = moment.player.cardsInHand.mapNotNull { cardManager.getCard(it.id)?.name },
                    dice = moment.player.diceInHand.values()
                )

            is Moment.REUSE_CARD ->
                ReuseCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.REUSE_CARD,
                    cardId = moment.card.id,
                    cardName = moment.card.name
                )

            is Moment.REUSE_DIE ->
                ReuseDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.REUSE_DIE,
                    die = moment.die.copy
                )

            is Moment.SET_TO_MAX ->
                AdjustDieToMax(
                    playerId = moment.player.id,
                    turn = gameTime.turn
                )

            is Moment.THORN_DAMAGE ->
                DeliverDamageEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.THORN_DAMAGE,
                    report = reportDamage(moment)
                )

            is Moment.TRASH_CARD ->
                TrashCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.TRASH_CARD,
                    card = moment.card
                )

            is Moment.TRASH_DIE ->
                TrashDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.TRASH_DIE,
                    dieSides = moment.die.sides
                )

            is Moment.TRASH_FOR_EFFECT ->
                TrashForEffect(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.TRASH_FOR_EFFECT,
                    card = moment.card.name,
                    status = moment.status
                )

            is Moment.UPGRADE_DIE ->
                UpgradeDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.UPGRADE_DIE,
                    newSides = moment.die.sides
                )

            is Moment.USE_FLOWERS ->
                UseFlowers(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    flowers = moment.flowers.joinToString(",") { it.name }
                )

            is Moment.USE_OPPONENT_CARD ->
                UseOpponentCardEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.USE_OPPONENT_CARD,
                    cardId = moment.card.id,
                    cardName = moment.card.name
                )

            is Moment.USE_OPPONENT_DIE ->
                UseOpponentDieEntry(
                    playerId = moment.player.id,
                    turn = gameTime.turn,
                    timeTaken = TimeTaken.USE_OPPONENT_DIE,
                    die = moment.die.copy
                )

        }
    }

    private fun reduceOf(effect: AppliedEffect): Int {
        return if (effect is AppliedEffect.MarketBenefit) {
            return effect.costReduction
        } else 0
    }

}
