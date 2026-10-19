package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class RaiseDiePlus1AndDoubleMatchingDiceBattle(
    private val chronicle: Chronicle
) {

    operator fun invoke(
        battle: Battle,
        player: Player,
        targetPlayer: Player,
        row: BattleStrikeRow,
        card: GameCard,
        target: Dice
    ) {
        val requested = target.dice
        if (requested.size !in 2..3) {
            warn(player, card)
            return
        }
        // This uses a snapshot as a stable read model before mutating the live grid. The search is intentionally
        // simple: scan from the indicated row, wrap through the strike rows, and mutate the first matching dice found.
        battle.snapshot()
        val dice = findMatchingDice(battle, targetPlayer, row, requested)
        if (dice == null) {
            warn(player, card)
            return
        }

        val success = when (dice.size) {
            2 -> resolveTwoDice(battle, targetPlayer, row, player, card, dice)
            3 -> resolveThreeDice(battle, targetPlayer, player, card, dice)
            else -> false
        }
        if (!success) return
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Raised one battle die by 1 and doubled matching battle dice",
                dice = Dice(dice.map { it.die })
            )
        )
    }

    private fun resolveTwoDice(
        battle: Battle,
        targetPlayer: Player,
        row: BattleStrikeRow,
        player: Player,
        card: GameCard,
        dice: List<LocatedDie>
    ): Boolean {
        val first = dice[0]
        val second = dice[1]
        if (first.die.value == second.die.value) {
            double(battle, targetPlayer, first)
            double(battle, targetPlayer, second)
            battle.raiseDie(targetPlayer, first.row, first.die, 1)
            return true
        } else {
            battle.raiseDie(targetPlayer, first.row, first.die, 1)
            if (first.die.value != second.die.value) {
                warn(player, card)
                return false
            }
            val refreshed = findMatchingDice(battle, targetPlayer, row, listOf(first.die, second.die))
            if (refreshed == null) {
                warn(player, card)
                return false
            }
            double(battle, targetPlayer, refreshed[0])
            double(battle, targetPlayer, refreshed[1])
            return true
        }
    }

    private fun resolveThreeDice(
        battle: Battle,
        targetPlayer: Player,
        player: Player,
        card: GameCard,
        dice: List<LocatedDie>
    ): Boolean {
        val first = dice[0]
        val second = dice[1]
        val third = dice[2]
        if (first.die.value != second.die.value) {
            warn(player, card)
            return false
        }
        double(battle, targetPlayer, first)
        double(battle, targetPlayer, second)
        battle.raiseDie(targetPlayer, third.row, third.die, 1)
        return true
    }

    private fun findMatchingDice(
        battle: Battle,
        targetPlayer: Player,
        startRow: BattleStrikeRow,
        requested: List<Die>
    ): List<LocatedDie>? {
        val consumed = mutableListOf<Die>()
        return requested.mapIndexed { index, target ->
            val rowOrder = rowsFrom(if (index == 0) startRow else startRow)
            val found = rowOrder.firstNotNullOfOrNull { row ->
                battle.grid.getSquare(targetPlayer.id, row).all
                    .filterIsInstance<BattleItem.DieItem>()
                    .firstOrNull { it.die == target && consumed.none { consumedDie -> consumedDie === it.die } }
                    ?.let { LocatedDie(row, it.die) }
            } ?: return null
            consumed.add(found.die)
            found
        }
    }

    private fun rowsFrom(start: BattleStrikeRow): List<BattleStrikeRow> {
        val rows = BattleStrikeRow.entries
        val index = rows.indexOf(start)
        return rows.drop(index) + rows.take(index)
    }

    private fun double(
        battle: Battle,
        targetPlayer: Player,
        die: LocatedDie
    ) {
        battle.setDieValue(targetPlayer, die.row, die.die, die.die.value * 2)
    }

    private fun warn(
        player: Player,
        card: GameCard
    ) {
        chronicle(
            Moment.Warning(
                player = player,
                type = WarningType.DOUBLE_MATCHING_DICE_INVALID_TARGET,
                card = card
            )
        )
    }

    private data class LocatedDie(
        val row: BattleStrikeRow,
        val die: Die
    )
}
