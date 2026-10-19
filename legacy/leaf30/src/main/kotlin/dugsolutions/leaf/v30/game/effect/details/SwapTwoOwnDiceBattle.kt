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
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieValue

class SwapTwoOwnDiceBattle(
    private val chronicle: Chronicle
) {
    private companion object {
        const val TARGET_DICE_COUNT = 2
    }

    operator fun invoke(
        battle: Battle,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?,
        row: BattleStrikeRow?,
        row2: BattleStrikeRow?
    ) {
        val firstRow = row ?: throw MainActionException("Battle swap requires the first battle row")
        val secondRow = row2 ?: throw MainActionException("Battle swap requires the second battle row")
        val targetDice = target?.dice?.diceInOrder.orEmpty()
        require(targetDice.size == TARGET_DICE_COUNT) {
            "Swap two own dice requires exactly $TARGET_DICE_COUNT dice"
        }

        val firstDie = findOwnDie(battle, player, firstRow, targetDice[0])
        val secondDie = findOwnDie(battle, player, secondRow, targetDice[1])
        if (firstDie == null || secondDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return
        }

        val firstUpdated = battle.setDieValue(player, firstRow, firstDie, secondDie.value)
        val secondUpdated = battle.setDieValue(player, secondRow, secondDie, firstDie.value)
        if (firstUpdated == null || secondUpdated == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return
        }

        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Swapped own dice values between $firstRow and $secondRow",
                dice = Dice(listOf(firstUpdated, secondUpdated))
            )
        )
    }

    private fun findOwnDie(
        battle: Battle,
        player: Player,
        row: BattleStrikeRow,
        requested: Die
    ): DieValue? {
        return battle.snapshot().columns
            .firstOrNull { it.playerId == player.id }
            ?.squares
            ?.getValue(row)
            ?.items
            ?.filterIsInstance<BattleItemSnapshot.DieItem>()
            ?.firstOrNull { it.die.equals(requested) }
            ?.die
    }
}
