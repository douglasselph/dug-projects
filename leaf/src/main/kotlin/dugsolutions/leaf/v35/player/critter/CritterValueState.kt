package dugsolutions.leaf.v35.player.critter

import dugsolutions.leaf.v35.tokens.Critter

/**
 * Player-local temporary Critter value rules.
 *
 * A Critter remains physically a BEE or WORM. Round effects can change what
 * that Critter contributes for this Player without changing the physical
 * Critter type. Critters gained later in the same round automatically use the
 * same current effective value.
 *
 * Two operations are intentionally distinct:
 *
 * - [boostForRound] adds to the current effective value and therefore stacks.
 *   Root Appreciation uses this: Worm 1 -> 3 -> 5 if resolved twice.
 *
 * - [setForRound] establishes an exact effective value for the round.
 *   This is useful for effects whose rule says a Critter "is worth X" rather
 *   than "is worth X more".
 */
class CritterValueState {
    private val roundValues = mutableMapOf<Critter, Int>()

    /** Current effective value of this Critter for this Player. */
    fun valueOf(critter: Critter): Int =
        roundValues[critter] ?: critter.baseValue

    /**
     * Adds [amount] to this Critter's current effective value for the rest of
     * the round. Repeated calls stack.
     */
    fun boostForRound(
        critter: Critter,
        amount: Int
    ) {
        require(amount > 0) {
            "Critter round boost must be positive: $amount"
        }

        roundValues[critter] =
            valueOf(critter) + amount
    }

    /**
     * Makes [critter] worth exactly [value] for the remainder of this round.
     */
    fun setForRound(
        critter: Critter,
        value: Int
    ) {
        require(value > 0) {
            "Critter round value must be positive: $value"
        }

        roundValues[critter] = value
    }

    fun hasRoundOverride(critter: Critter): Boolean =
        critter in roundValues

    val overrides: Map<Critter, Int>
        get() = roundValues.toMap()

    /** Called by round cleanup after all uses of this round's boost are done. */
    fun clearRound() {
        roundValues.clear()
    }
}
