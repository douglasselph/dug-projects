package dugsolutions.leaf.v35.player.dice

import dugsolutions.leaf.v35.random.die.Dice
import dugsolutions.leaf.v35.random.die.Die
import java.util.IdentityHashMap

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
    /** Optional research hooks; fire only for ownership changes, not zone moves. */
    var onAcquired: ((Long, Int) -> Unit)? = null
    var onRemoved: ((Long, Int, String) -> Unit)? = null
    var onUpgrade: ((Long, Int, Int) -> Unit)? = null

    fun recordRemoval(die: Die, reason: String) {
        onRemoved?.invoke(stableDieId(die), die.sides, reason)
    }

    private fun recordAcquired(die: Die) {
        onAcquired?.invoke(stableDieId(die), die.sides)
    }

    /** A die is leaving this player's ownership, rather than changing zones. */
    fun recordTransferOut(die: Die, reason: String) {
        recordRemoval(die, reason)
        stableDieIds.remove(die)
    }

    /** A die received from another player becomes a new acquisition for this owner. */
    fun recordTransferIn(die: Die) {
        recordAcquired(die)
    }

    private val _supply = Dice(supply)
    private val _hand = Dice(hand)
    private val _discard = Dice(discard)

    // Game-local physical identity, independent of the die's mutable face value.
    // Starting dice receive slot identities before any decisions are made.
    private val stableDieIds = IdentityHashMap<Die, Long>()
    private var nextStableDieId = 1L

    init {
        (supply + hand + discard).forEach { stableDieId(it) }
    }

    fun stableDieId(die: Die): Long =
        stableDieIds.getOrPut(die) { nextStableDieId++ }

    /** Transfer the event identity when an Upgrade exchanges the physical die.
     * This changes only replay identity, never actual die ownership or rules.
     */
    fun inheritStableDieId(source: Die, replacement: Die) {
        val identity = stableDieId(source)
        val previous = stableDieIds[replacement]
        require(previous == null || previous == identity) {
            "Replacement die already has a different event identity"
        }
        stableDieIds[replacement] = identity
        stableDieIds.remove(source)
        onUpgrade?.invoke(identity, source.sides, replacement.sides)
    }



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
        recordAcquired(die)
        _supply.add(die)
        return this
    }

    fun addAllToSupply(dice: Iterable<Die>): PlayerDice {
        val newDice = dice.toList()
        newDice.forEach(::recordAcquired)
        _supply.addAll(newDice)
        return this
    }

    /**
     * Special-rule entry directly into the Dice Hand.
     *
     * Normal Draw should use [draw].
     */
    fun addToHand(die: Die): PlayerDice {
        recordAcquired(die)
        _hand.add(die)
        return this
    }

    /**
     * Normal destination for bought/gained dice.
     */
    fun addToDiscard(die: Die): PlayerDice {
        recordAcquired(die)
        _discard.add(die)
        return this
    }

    fun addAllToDiscard(dice: Iterable<Die>): PlayerDice {
        val newDice = dice.toList()
        newDice.forEach(::recordAcquired)
        _discard.addAll(newDice)
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
    fun draw(rollDie: (Die) -> Unit = { it.roll() }): Die? {
        if (_supply.dice.isEmpty()) {
            refillSupply()
        }

        val die = _supply.drawLowest() ?: return null
        rollDie(die)
        _hand.add(die)
        return die
    }

    /**
     * Removes one matching die from Hand and returns the supplied die value
     * when successful. Equivalent dice are intentionally interchangeable.
     */
    /** Removes one matching die from Supply. */
    fun removeFromSupply(die: Die): Die? {
        val exact = _supply.dice.firstOrNull { it === die }
        if (exact != null) {
            return if (_supply.removeExact(exact)) exact else null
        }

        val equivalent = _supply.dice.firstOrNull { it == die } ?: return null
        return if (_supply.removeExact(equivalent)) equivalent else null
    }

    fun removeFromHand(die: Die): Die? {
        val exact = _hand.dice.firstOrNull { it === die }
        if (exact != null) {
            return if (_hand.removeExact(exact)) exact else null
        }

        val equivalent = _hand.dice.firstOrNull { it == die } ?: return null
        return if (_hand.removeExact(equivalent)) equivalent else null
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
     * Exchanges two exact live Hand dice between players while preserving each
     * Hand's list position. This is intended for cross-player Battle effects
     * such as Pollen Theft after both dice have already been validated.
     *
     * Returns false without mutation if either exact die is not currently in
     * the expected Hand, the two [PlayerDice] objects are the same, or the
     * supplied die references are identical.
     */
    fun swapExactHandDieWith(
        other: PlayerDice,
        ownDie: Die,
        otherDie: Die
    ): Boolean {
        if (this === other || ownDie === otherDie) return false

        val ownHand = _hand.dice.toMutableList()
        val otherHand = other._hand.dice.toMutableList()
        val ownIndex = ownHand.indexOfFirst { it === ownDie }
        val otherIndex = otherHand.indexOfFirst { it === otherDie }

        if (ownIndex < 0 || otherIndex < 0) return false

        ownHand[ownIndex] = otherDie
        otherHand[otherIndex] = ownDie
        _hand.set(ownHand)
        other._hand.set(otherHand)
        // Record actual ownership changes only after both Hands are updated.
        // The two players' local die IDs are independent; a transferred die
        // receives a new ID in its destination ledger.
        recordTransferOut(ownDie, "SWAPPED_OUT")
        other.recordTransferOut(otherDie, "SWAPPED_OUT")
        recordTransferIn(otherDie)
        other.recordTransferIn(ownDie)
        return true
    }

    /**
     * Removes one matching die from Discard and returns the supplied die value
     * when successful. Equivalent dice are intentionally interchangeable.
     */
    fun removeFromDiscard(die: Die): Die? {
        val exact = _discard.dice.firstOrNull { it === die }
        if (exact != null) {
            return if (_discard.removeExact(exact)) exact else null
        }

        val equivalent = _discard.dice.firstOrNull { it == die } ?: return null
        return if (_discard.removeExact(equivalent)) equivalent else null
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
