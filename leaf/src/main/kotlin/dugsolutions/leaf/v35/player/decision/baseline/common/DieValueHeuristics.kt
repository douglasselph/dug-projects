package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.player.decision.context.DieView
import kotlin.math.max
import kotlin.math.min

/**
 * Pure numeric helpers for reasoning about one visible die.
 *
 * These functions deliberately evaluate face value only. Roll Rewards are a
 * separate strategic consideration and should be scored explicitly by the
 * caller rather than hidden inside a die-value calculation.
 */
object DieValueHeuristics {

    /** Expected face value of a fair [sides]-sided die. */
    fun expectedRoll(sides: Int): Double {
        require(sides > 0) { "Die sides must be positive: $sides" }
        return (sides + 1) / 2.0
    }

    /** Actual value gained by Raise +[amount], respecting the die maximum. */
    fun actualRaiseGain(
        sides: Int,
        value: Int,
        amount: Int
    ): Int {
        requireDie(sides, value)
        require(amount >= 0) { "Raise amount cannot be negative: $amount" }
        if (value >= sides) return 0
        return min(sides, value + amount) - value
    }

    fun actualRaiseGain(
        die: DieView,
        amount: Int
    ): Int = actualRaiseGain(die.sides, die.value, amount)

    /** Value gained by setting this die to its normal maximum face. */
    fun setToMaximumGain(
        sides: Int,
        value: Int
    ): Int {
        requireDie(sides, value)
        return max(0, sides - value)
    }

    fun setToMaximumGain(die: DieView): Int =
        setToMaximumGain(die.sides, die.value)

    /**
     * Opposite face for a flippable die, or null for D4 and smaller dice.
     *
     * Flip is a face operation, so a temporarily boosted value above the
     * physical side count is rejected instead of inventing a non-face result.
     */
    fun flippedValue(
        sides: Int,
        value: Int
    ): Int? {
        requireDie(sides, value)
        if (sides <= 4) return null
        require(value <= sides) {
            "Cannot evaluate opposite face for boosted D$sides value $value"
        }
        return (sides + 1) - value
    }

    /** Signed value change from flipping; non-flippable D4s have gain 0. */
    fun flipGain(
        sides: Int,
        value: Int
    ): Int =
        flippedValue(sides, value)?.minus(value) ?: 0

    fun flipGain(die: DieView): Int =
        flipGain(die.sides, die.value)

    /** Signed expected face-value change if the current result is rerolled. */
    fun expectedRerollGain(
        sides: Int,
        value: Int
    ): Double {
        requireDie(sides, value)
        return expectedRoll(sides) - value
    }

    fun expectedRerollGain(die: DieView): Double =
        expectedRerollGain(die.sides, die.value)

    /**
     * Expected gain when a reroll may be rejected after it is seen, as with a
     * Butterfly. This is E[max(original, reroll)] - original.
     */
    fun expectedKeepBestRerollGain(
        sides: Int,
        value: Int
    ): Double {
        requireDie(sides, value)
        if (value >= sides) return 0.0

        val gainSum = ((value + 1)..sides).sumOf { rolled ->
            rolled - value
        }
        return gainSum.toDouble() / sides
    }

    fun expectedKeepBestRerollGain(die: DieView): Double =
        expectedKeepBestRerollGain(die.sides, die.value)

    private fun requireDie(
        sides: Int,
        value: Int
    ) {
        require(sides > 0) { "Die sides must be positive: $sides" }
        require(value > 0) { "Die value must be positive: $value" }
    }
}
