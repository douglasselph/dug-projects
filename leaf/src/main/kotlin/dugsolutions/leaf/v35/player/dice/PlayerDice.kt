package dugsolutions.leaf.v35.player.dice

import dugsolutions.leaf.v35.random.die.Dice
import dugsolutions.leaf.v35.random.die.Die

/**
 * Owns the three normal dice zones for one player:
 *
 * - Player Dice Supply
 * - Dice Hand
 * - Dice Discard Bin
 *
 * PlayerDice owns movement between these zones. It does not own effect-specific
 * behavior such as Raise, Set, Flip, or special rerolls.
 */
class PlayerDice(
    supply: List<Die> = emptyList(),
    hand: List<Die> = emptyList(),
    discard: List<Die> = emptyList()
) {
    private val _supply = Dice(supply)
    private val _hand = Dice(hand)
    private val _discard = Dice(discard)

    /**
     * Structural snapshots of each zone.
     *
     * The lists themselves cannot modify PlayerDice membership, but the Die
     * objects remain the live dice so gameplay effects may change their values.
     */
    val supply: List<Die>
        get() = _supply.dice

    val hand: List<Die>
        get() = _hand.dice

    val discard: List<Die>
        get() = _discard.dice

    val supplySize: Int
        get() = supply.size

    val handSize: Int
        get() = hand.size

    val discardSize: Int
        get() = discard.size

    val isSupplyEmpty: Boolean
        get() = supply.isEmpty()

    val isHandEmpty: Boolean
        get() = hand.isEmpty()

    val isDiscardEmpty: Boolean
        get() = discard.isEmpty()

    /**
     * Setup/special-rule entry into the Player Dice Supply.
     */
    fun addToSupply(die: Die): PlayerDice {
        _supply.add(die)
        return this
    }

    fun addAllToSupply(dice: Iterable<Die>): PlayerDice {
        _supply.addAll(dice.toList())
        return this
    }

    /**
     * Special-rule entry directly into the Dice Hand.
     *
     * Normal Draw should use [draw].
     */
    fun addToHand(die: Die): PlayerDice {
        _hand.add(die)
        return this
    }

    /**
     * Normal destination for bought/gained dice.
     */
    fun addToDiscard(die: Die): PlayerDice {
        _discard.add(die)
        return this
    }

    fun addAllToDiscard(dice: Iterable<Die>): PlayerDice {
        _discard.addAll(dice.toList())
        return this
    }

    /**
     * Performs the normal v35 Draw lifecycle:
     *
     * 1. If Supply is empty, refill it from Discard.
     * 2. Take the lowest-sided available die.
     * 3. Roll it.
     * 4. Place it in Hand.
     *
     * Returns null when both Supply and Discard are empty.
     *
     * Roll Rewards are intentionally NOT handled here. The caller is
     * responsible for resolving them after the returned die is rolled.
     */
    fun draw(): Die? {
        if (_supply.dice.isEmpty()) {
            refillSupply()
        }

        val die = _supply.drawLowest() ?: return null
        die.roll()
        _hand.add(die)
        return die
    }

    /**
     * Removes one matching die from Hand and returns the supplied die value
     * when successful. Equivalent dice are intentionally interchangeable.
     */
    fun removeFromHand(die: Die): Die? {
        return if (_hand.remove(die)) die else null
    }

    /**
     * Removes this exact live die instance from Hand.
     *
     * Most simulation rules intentionally treat equivalent dice as
     * interchangeable and may use [removeFromHand]. Battle Grid location is
     * different: two equivalent dice can occupy different Strike Squares, so
     * Battle cleanup/Trash must preserve exact identity.
     */
    fun removeExactFromHand(die: Die): Die? {
        val current = _hand.dice.firstOrNull { it === die } ?: return null
        return if (_hand.removeExact(current)) current else null
    }

    /**
     * Removes one matching die from Discard and returns the supplied die value
     * when successful. Equivalent dice are intentionally interchangeable.
     */
    fun removeFromDiscard(die: Die): Die? {
        return if (_discard.remove(die)) die else null
    }

    /**
     * Cleanup behavior: all remaining Hand dice move to Discard.
     *
     * Dice retain their current values during the transfer.
     */
    fun discardHand() {
        _discard.addAll(_hand.dice)
        _hand.clear()
    }

    /**
     * Clears all player-owned dice zones.
     *
     * Intended primarily for setup/reset/testing rather than routine gameplay.
     */
    fun clear() {
        _supply.clear()
        _hand.clear()
        _discard.clear()
    }

    private fun refillSupply() {
        if (_discard.dice.isEmpty()) return

        _supply.addAll(_discard.dice)
        _discard.clear()
    }
}
