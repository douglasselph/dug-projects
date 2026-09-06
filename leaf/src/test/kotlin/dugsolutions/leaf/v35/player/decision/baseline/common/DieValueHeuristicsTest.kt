package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.player.decision.context.DieView
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DieValueHeuristicsTest {

    @Test
    fun actualRaiseGain_stopsAtMaximum() {
        assertEquals(
            2,
            DieValueHeuristics.actualRaiseGain(
                DieView(index = 0, sides = 20, value = 18),
                amount = 4
            )
        )
        assertEquals(0, DieValueHeuristics.actualRaiseGain(20, 20, 4))
    }

    @Test
    fun setToMaximumGain_returnsOnlyRealIncrease() {
        assertEquals(19, DieValueHeuristics.setToMaximumGain(20, 1))
        assertEquals(0, DieValueHeuristics.setToMaximumGain(8, 8))
    }

    @Test
    fun flipGain_isSignedAndD4IsNotFlippable() {
        assertEquals(19, DieValueHeuristics.flipGain(20, 1))
        assertEquals(-3, DieValueHeuristics.flipGain(6, 5))
        assertEquals(0, DieValueHeuristics.flipGain(4, 1))
        assertNull(DieValueHeuristics.flippedValue(4, 1))
    }

    @Test
    fun expectedRollAndRerollGain_useFairDiscreteExpectation() {
        assertEquals(3.5, DieValueHeuristics.expectedRoll(6))
        assertEquals(9.5, DieValueHeuristics.expectedRerollGain(20, 1))
        assertEquals(-9.5, DieValueHeuristics.expectedRerollGain(20, 20))
    }

    @Test
    fun expectedKeepBestRerollGain_modelsButterflyChoice() {
        // D6=4 only benefits from rerolling a 5 or 6: (1 + 2) / 6.
        assertEquals(0.5, DieValueHeuristics.expectedKeepBestRerollGain(6, 4))
        assertEquals(0.0, DieValueHeuristics.expectedKeepBestRerollGain(6, 6))
    }

    @Test
    fun flippedValue_rejectsNonFaceBoostedValues() {
        assertFailsWith<IllegalArgumentException> {
            DieValueHeuristics.flippedValue(20, 21)
        }
    }
}
