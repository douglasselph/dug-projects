package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.table.Table

class SetDieToMatchAnotherBattle(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        table: Table,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?,
        row: BattleStrikeRow?
    ) {
        val targetRow = row ?: throw MainActionException("Battle set die requires a battle row")
        val targetPlayerDie = target as? ExecuteTarget.PlayerDie
            ?: throw MainActionException("Battle set die requires player dice target")
        val targetDice = targetPlayerDie.dice.diceInOrder
        require(targetDice.size == TARGET_DICE_COUNT) {
            "Set die to match another requires exactly $TARGET_DICE_COUNT dice"
        }
        // Target dice order is significant: [0] is the source die to copy from, [1] is the die to change.
        val sourceRequest = targetDice[0]
        val targetRequest = targetDice[1]
        val square = table.battle.grid.getSquare(targetPlayerDie.player.id, targetRow)
        val sourceDie = findDie(square.all, sourceRequest)
            ?: throw MainActionException("Set die source was not found in battle grid")
        val targetDie = findDie(square.all, targetRequest)
            ?: throw MainActionException("Set die target was not found in battle grid")

        if (!table.battle.setDieValue(targetPlayerDie.player, targetRow, targetDie, sourceDie.value)) {
            throw MainActionException("Set die target was not found in battle grid")
        }
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Set one battle die on $targetRow to match another battle die",
                dice = Dice(listOf(sourceDie, targetDie))
            )
        )
    }

    private fun findDie(
        items: List<BattleItem>,
        target: Die
    ): Die? {
        return items.filterIsInstance<BattleItem.DieItem>()
            .firstOrNull { it.die == target }
            ?.die
    }

    private companion object {
        const val TARGET_DICE_COUNT = 2
    }
}
