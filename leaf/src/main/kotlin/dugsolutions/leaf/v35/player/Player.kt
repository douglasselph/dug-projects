package dugsolutions.leaf.v35.player

import dugsolutions.leaf.v35.player.creature.Creature
import dugsolutions.leaf.v35.player.critter.CritterValueState
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.player.wisp.WispHand
import dugsolutions.leaf.v35.tokens.Butterflies
import dugsolutions.leaf.v35.tokens.Critters
import dugsolutions.leaf.v35.tokens.Tokens

/**
 * Aggregate root for state owned by one player.
 *
 * Player owns its decision-policy composition so different players in the
 * same simulation may use different strategies. Player itself does not invoke
 * those strategies or execute their results; gameplay coordinators do that.
 *
 * Player deliberately does not duplicate the APIs of its child state objects.
 */
class Player(
    val id: PlayerId,
    val decisions: DecisionDirector,
    val creature: Creature = Creature(),
    val dice: PlayerDice = PlayerDice(),
    val critters: Critters = Critters(),
    val critterValues: CritterValueState = CritterValueState(),
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
