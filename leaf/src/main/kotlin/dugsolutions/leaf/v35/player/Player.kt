package dugsolutions.leaf.v35.player

import dugsolutions.leaf.v35.player.creature.Creature
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.player.wisp.WispHand
import dugsolutions.leaf.v35.tokens.Butterflies
import dugsolutions.leaf.v35.tokens.Critters
import dugsolutions.leaf.v35.tokens.Tokens

/**
 * Aggregate root for state owned by one player.
 *
 * Player deliberately does not duplicate the APIs of its child state objects.
 * Gameplay coordinators and effects should operate through the appropriate
 * owned component:
 *
 * - creature
 * - dice
 * - critters
 * - tokens
 * - butterflies
 * - wisps
 *
 * Decisions and Chronicle are intentionally not part of this initial state
 * foundation.
 */
class Player(
    val id: PlayerId,
    val creature: Creature = Creature(),
    val dice: PlayerDice = PlayerDice(),
    val critters: Critters = Critters(),
    val tokens: Tokens = Tokens(),
    val butterflies: Butterflies = Butterflies(),
    val wisps: WispHand = WispHand()
) {
    var vp: Int = 0
        private set

    fun addVp(amount: Int) {
        require(amount > 0) {
            "VP amount must be positive: $amount"
        }
        vp += amount
    }

    fun resetVp() {
        vp = 0
    }
}
