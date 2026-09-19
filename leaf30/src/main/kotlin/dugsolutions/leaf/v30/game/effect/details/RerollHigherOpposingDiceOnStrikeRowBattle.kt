package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
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
import dugsolutions.leaf.v30.random.die.DieValue

class RerollHigherOpposingDiceOnStrikeRowBattle(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        battle: Battle,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?,
        row: BattleStrikeRow?
    ) {
        val targetRow = row ?: throw MainActionException("Battle reroll opposing dice requires a battle row")
        val targetDie = target?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.REROLL_TARGET_MISSING, card = card))
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
            chronicle(Moment.Warning(player = player, type = WarningType.REROLL_DIE_NOT_FOUND, card = card))
            return
        }

        val rerolled = snapshot.columns
            .filterNot { it.playerId == player.id }
            .flatMap { column ->
                column.squares.getValue(targetRow).items
                    .filterIsInstance<BattleItemSnapshot.DieItem>()
                    .filter { item -> item.die.value > playerDie.value }
                    .map { item -> column.playerId to item.die }
            }
            .mapNotNull { (playerId, die) ->
                battle.rerollDie(player = Player(id = playerId), row = targetRow, die = die)
            }

        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Rerolled ${rerolled.size} higher opposing die/dice on $targetRow",
                dice = Dice(rerolled)
            )
        )
    }
}
