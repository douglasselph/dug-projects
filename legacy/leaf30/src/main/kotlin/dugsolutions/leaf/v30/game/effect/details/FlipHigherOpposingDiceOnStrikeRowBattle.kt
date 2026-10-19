package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleGridSnapshot
import dugsolutions.leaf.v30.battle.domain.BattleItemSnapshot
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieValue

class FlipHigherOpposingDiceOnStrikeRowBattle(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        battle: Battle,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?,
        row: BattleStrikeRow?
    ) {
        val targetRow = row ?: throw MainActionException("Battle flip higher opposing dice requires a battle row")
        val targetDie = target?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.FLIP_TARGET_MISSING, card = card))
            return
        }

        val snapshot = battle.snapshot()
        val playerDie = snapshot.columns
            .firstOrNull { it.playerId == player.id }
            ?.squares
            ?.getValue(targetRow)
            ?.items
            ?.filterIsInstance<BattleItemSnapshot.DieItem>()
            ?.firstOrNull { it.die.equals(targetDie) }
            ?.die
        if (playerDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.FLIP_DIE_NOT_FOUND, card = card))
            return
        }

        val flipped = flipOpposingHigherDice(battle, snapshotDice(snapshot, player.id, playerDie.value))
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Flipped ${flipped.size} higher opposing die/dice on $targetRow",
                dice = Dice(flipped)
            )
        )
    }

    private fun snapshotDice(
        snapshot: BattleGridSnapshot,
        playerId: Int,
        targetValue: Int
    ): List<Pair<Int, Pair<BattleStrikeRow, DieValue>>> {
        return snapshot.columns
            .filterNot { it.playerId == playerId }
            .flatMap { column ->
                column.squares.flatMap { (row, square) ->
                    square.items
                        .filterIsInstance<BattleItemSnapshot.DieItem>()
                        .filter { item -> item.die.value > targetValue }
                        .map { item -> column.playerId to (row to item.die) }
                }
            }
    }

    private fun flipOpposingHigherDice(
        battle: Battle,
        dice: List<Pair<Int, Pair<BattleStrikeRow, DieValue>>>
    ): List<Die> {
        return dice.mapNotNull { (playerId, rowAndDie) ->
            val (row, die) = rowAndDie
            battle.flipDie(Player(id = playerId), row, die)
        }
    }
}
