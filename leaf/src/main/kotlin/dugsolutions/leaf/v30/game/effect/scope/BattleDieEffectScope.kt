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
    private val rows: List<BattleStrikeRow>
) : DieEffectScope {

    constructor(
        battle: Battle,
        actingPlayer: Player,
        targetPlayer: Player,
        row: BattleStrikeRow
    ) : this(
        battle = battle,
        actingPlayer = actingPlayer,
        targetPlayer = targetPlayer,
        rows = listOf(row)
    )

    override val locationDescription: String
        get() = if (rows.size == 1) {
            "player ${targetPlayer.id}'s ${rows.first()} battle square"
        } else {
            "player ${targetPlayer.id}'s battle squares ${rows.joinToString()}"
        }

    override fun allDice(): Dice {
        val dice = rows.flatMap { row ->
            battle.grid.getSquare(targetPlayer.id, row).all
        }
            .filterIsInstance<BattleItem.DieItem>()
            .map { it.die }
        return Dice(dice)
    }

    override fun hasDie(die: Die?, index: Int?): Boolean {
        return battle.hasDie(targetPlayer, rowFor(index), die)
    }

    override fun findDie(die: Die?, index: Int?): Die? {
        if (die == null) return null
        return battle.grid.getSquare(targetPlayer.id, rowFor(index)).all
            .filterIsInstance<BattleItem.DieItem>()
            .firstOrNull { it.die == die }
            ?.die
    }

    override fun reroll(die: Die, index: Int?): Die? {
        return battle.rerollDie(targetPlayer, rowFor(index), die)
    }

    override fun raise(die: Die, amount: Int, index: Int?): Die? {
        return battle.raiseDie(targetPlayer, rowFor(index), die, amount)
    }

    override fun setValue(die: Die, value: Int, index: Int?): Die? {
        if (!battle.setDieValue(targetPlayer, rowFor(index), die, value)) return null
        return findDie(die, index)
    }

    private fun rowFor(index: Int?): BattleStrikeRow {
        return rows.getOrNull(index ?: 0) ?: rows.first()
    }
}
