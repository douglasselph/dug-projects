package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleItemSnapshot
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.DieValue
import dugsolutions.leaf.v30.random.die.di.DieFactory

class GainD4OrReturnD4RaiseDiePlus4Battle(
    private val chronicle: Chronicle,
    private val dieFactory: DieFactory
) {
    operator fun invoke(
        battle: Battle,
        grove: Grove,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?,
        row: BattleStrikeRow?
    ) {
        val targetRow = row ?: throw MainActionException("Battle gain/return D4 requires a battle row")
        val targetDice = target?.dice?.diceInOrder.orEmpty()
        if (targetDice.isEmpty()) {
            gainD4(battle, grove, player, card, targetRow)
        } else {
            returnD4AndRaiseDie(battle, grove, player, card, targetRow, targetDice)
        }
    }

    private fun gainD4(
        battle: Battle,
        grove: Grove,
        player: Player,
        card: GameCard,
        row: BattleStrikeRow
    ) {
        if (!grove.remove(DieSides.D4)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, card = card))
            return
        }
        val d4 = dieFactory(DieSides.D4).adjustTo(D4_MAX_VALUE)
        if (!battle.add(player, row, d4)) {
            grove.add(DieSides.D4)
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, card = card))
            return
        }
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Gained a D4 from the Grove onto $row with value $D4_MAX_VALUE",
                dice = Dice(listOf(d4))
            )
        )
    }

    private fun returnD4AndRaiseDie(
        battle: Battle,
        grove: Grove,
        player: Player,
        card: GameCard,
        row: BattleStrikeRow,
        targetDice: List<Die>
    ) {
        val dieToRaise = targetDice.first()
        val d4ToReturn = findD4ToReturn(battle, player, targetDice.getOrNull(1))
        if (d4ToReturn == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, card = card))
            return
        }
        if (!battle.remove(player, d4ToReturn.row, d4ToReturn.die)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, card = card))
            return
        }
        grove.add(DieSides.D4)
        val raised = battle.raiseDie(player, row, dieToRaise, RAISE_AMOUNT)
        if (raised == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return
        }
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Returned a D4 from the battle grid to the Grove and raised a battle die on $row by $RAISE_AMOUNT",
                dice = Dice(listOf(d4ToReturn.die.dieFrom(dieFactory), raised))
            )
        )
    }

    private fun findD4ToReturn(
        battle: Battle,
        player: Player,
        requested: Die?
    ): D4Selection? {
        if (requested?.sides == D4_SIDES) {
            BattleStrikeRow.entries.forEach { row ->
                val found = battle.grid.getSquare(player.id, row).all
                    .filterIsInstance<BattleItem.DieItem>()
                    .firstOrNull { it.die == requested }
                    ?.die
                if (found != null) return D4Selection(row, found.copy)
            }
        }
        return battle.snapshot().columns
            .firstOrNull { it.playerId == player.id }
            ?.squares
            ?.flatMap { (row, square) ->
                square.items
                    .filterIsInstance<BattleItemSnapshot.DieItem>()
                    .map { row to it.die }
            }
            ?.filter { (_, die) -> die.sides == D4_SIDES }
            ?.minByOrNull { (_, die) -> die.value }
            ?.let { (row, die) -> D4Selection(row, die) }
    }

    private data class D4Selection(
        val row: BattleStrikeRow,
        val die: DieValue
    )

    private companion object {
        const val D4_SIDES = 4
        const val D4_MAX_VALUE = 4
        const val RAISE_AMOUNT = 4
    }
}
