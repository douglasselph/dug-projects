package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice

class ReduceOpposingDiceOnStrikeRowBy3Battle(
    private val chronicle: Chronicle
) {

    private companion object {
        const val REDUCE_AMOUNT = -3
    }

    operator fun invoke(
        battle: Battle,
        player: Player,
        card: GameCard,
        row: BattleStrikeRow?
    ) {
        val targetRow = row ?: throw MainActionException("Battle reduce opposing dice requires a target row")
        val reducedDice = battle.grid.playerIdsInGridOrder
            .filter { playerId -> playerId != player.id }
            .flatMap { playerId ->
                battle.grid.getSquare(playerId, targetRow).all
                    .filterIsInstance<BattleItem.DieItem>()
                    .map { item ->
                        item.die.adjustBy(REDUCE_AMOUNT)
                    }
            }

        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Reduced opposing dice on $targetRow by ${-REDUCE_AMOUNT}",
                dice = Dice(reducedDice)
            )
        )
    }
}
