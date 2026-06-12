package dugsolutions.leaf.v30.game.effect.scope

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

/**
 * Abstraction for card effects that mutate dice without needing to know whether
 * those dice currently live in a player's hand or on the battle grid.
 */
interface DieEffectScope {

    val actingPlayer: Player
    val targetPlayer: Player
    val locationDescription: String

    fun allDice(): Dice
    fun hasDie(die: Die?): Boolean
    fun findDie(die: Die?): Die?
    fun reroll(die: Die): Die?
    fun raise(die: Die, amount: Int): Die?
    fun setValue(die: Die, value: Int): Die?
}
