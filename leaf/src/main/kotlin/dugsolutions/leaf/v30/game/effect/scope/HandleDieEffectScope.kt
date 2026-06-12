package dugsolutions.leaf.v30.game.effect.scope

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

/**
 * Hand-backed die scope used by cultivation effects.
 */
class HandleDieEffectScope(
    override val actingPlayer: Player,
    override val targetPlayer: Player = actingPlayer
) : DieEffectScope {

    override val locationDescription: String
        get() = "player ${targetPlayer.id}'s hand"

    override fun allDice(): Dice {
        return targetPlayer.diceHand
    }

    override fun hasDie(die: Die?): Boolean {
        return targetPlayer.diceHand.hasDie(die)
    }

    override fun findDie(die: Die?): Die? {
        if (die == null) return null
        return targetPlayer.diceHand.dice.firstOrNull { it == die }
    }

    override fun reroll(die: Die): Die? {
        return targetPlayer.rerollDie(die)
    }

    override fun raise(die: Die, amount: Int): Die? {
        return targetPlayer.raiseDie(die, amount)
    }

    override fun setValue(die: Die, value: Int): Die? {
        val target = findDie(die) ?: return null
        target.adjustTo(value)
        return target
    }
}
