package dugsolutions.leaf.v30.game.effect.scope

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

/**
 * Battle-grid-backed die scope used by battle effects.
 */
class BattleDieEffectScope(
    private val battle: Battle,
    override val actingPlayer: Player,
    override val targetPlayer: Player,
    private val row: BattleStrikeRow
) : DieEffectScope {

    override val locationDescription: String
        get() = "player ${targetPlayer.id}'s $row battle square"

    override fun allDice(): Dice {
        val dice = battle.grid.getSquare(targetPlayer.id, row).all
            .filterIsInstance<BattleItem.DieItem>()
            .map { it.die }
        return Dice(dice)
    }

    override fun hasDie(die: Die?): Boolean {
        return battle.hasDie(targetPlayer, row, die)
    }

    override fun findDie(die: Die?): Die? {
        if (die == null) return null
        return battle.grid.getSquare(targetPlayer.id, row).all
            .filterIsInstance<BattleItem.DieItem>()
            .firstOrNull { it.die == die }
            ?.die
    }

    override fun reroll(die: Die): Die? {
        return battle.rerollDie(targetPlayer, row, die)
    }

    override fun raise(die: Die, amount: Int): Die? {
        return battle.raiseDie(targetPlayer, row, die, amount)
    }

    override fun setValue(die: Die, value: Int): Die? {
        if (!battle.setDieValue(targetPlayer, row, die, value)) return null
        return findDie(die)
    }
}
