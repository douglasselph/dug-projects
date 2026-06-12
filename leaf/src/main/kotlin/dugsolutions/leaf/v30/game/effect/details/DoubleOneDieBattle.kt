package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.table.Table

class DoubleOneDieBattle(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        table: Table,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?,
        row: BattleStrikeRow?
    ) {
        val targetRow = row ?: throw MainActionException("Battle double die requires a battle row")
        val targetPlayerDie = target as? ExecuteTarget.PlayerDie
        if (targetPlayerDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_TARGET_MISSING, card = card))
            return
        }
        val targetDie = targetPlayerDie.dice.firstDie
        if (!table.battle.hasDie(targetPlayerDie.player, targetRow, targetDie)) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return
        }
        val raised = table.battle.raiseDie(
            player = targetPlayerDie.player,
            row = targetRow,
            die = targetDie ?: throw MainActionException("Double target die was not found"),
            amount = targetDie.value
        ) ?: return
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Doubled one battle die on $targetRow",
                dice = Dice(listOf(raised))
            )
        )
    }
}
