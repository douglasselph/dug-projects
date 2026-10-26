package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics

/**
 * C03: incremental spending pips, not legacy priority points.
 * A null draw indicates there is no die to draw (as opposed to a zero-value draw).
 * Predictions use the public supply/refill ordering and do not consume RNG.
 */
object CultivationPipOutcome {
    private val supportedSides = setOf(4, 6, 8, 10, 12, 20)

    fun expectedNewDie(sides: Int): Double {
        require(sides in supportedSides) { "Unsupported die for Cultivation draw: D$sides" }
        return DieValueHeuristics.expectedRoll(sides)
    }

    fun nextDraw(supply: List<Int>, discard: List<Int>): Double? =
        ExpectedDrawProjection.drawSides(supply, discard, 1).singleOrNull()?.let(::expectedNewDie)

    fun consecutiveDraws(supply: List<Int>, discard: List<Int>, count: Int): Double =
        ExpectedDrawProjection.drawSides(supply, discard, count).sumOf(::expectedNewDie)

    /** Signed net change is retained: a face transformation can lose spending pips. */
    fun changedExistingDie(oldValue: Int, newValue: Int, sides: Int): Int {
        require(sides in supportedSides && oldValue in 1..sides && newValue in 1..sides)
        return newValue - oldValue
    }

    /** A Raise cannot exceed the physical maximum; there is no reward for unreachable pips. */
    fun raisedExistingDie(sides: Int, value: Int, amount: Int): Int =
        DieValueHeuristics.actualRaiseGain(sides, value, amount).also {
            require(sides in supportedSides) { "Unsupported die: D$sides" }
        }
}
