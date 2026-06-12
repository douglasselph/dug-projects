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

class FlipDieToOppositeFaceBattle(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        table: Table,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?,
        row: BattleStrikeRow?
    ) {
        val targetRow = row ?: throw MainActionException("Battle flip requires a battle row")
        val targetPlayerDie = target as? ExecuteTarget.PlayerDie
        val targetDie = targetPlayerDie?.dice?.firstDie
        if (targetPlayerDie == null || targetDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.FLIP_TARGET_MISSING, card = card))
            return
        }
        if (!table.battle.hasDie(targetPlayerDie.player, targetRow, targetDie)) {
            chronicle(Moment.Warning(player = player, type = WarningType.FLIP_DIE_NOT_FOUND, card = card))
            return
        }
        val flippedValue = if (targetDie.sides > 4) {
            (targetDie.sides + 1) - targetDie.value
        } else {
            targetDie.value
        }
        if (!table.battle.setDieValue(targetPlayerDie.player, targetRow, targetDie, flippedValue)) {
            chronicle(Moment.Warning(player = player, type = WarningType.FLIP_DIE_NOT_FOUND, card = card))
            return
        }
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Flipped player ${targetPlayerDie.player.id}'s battle die on $targetRow to its opposite face",
                dice = Dice(listOf(targetDie.adjustTo(flippedValue)))
            )
        )
    }
}
