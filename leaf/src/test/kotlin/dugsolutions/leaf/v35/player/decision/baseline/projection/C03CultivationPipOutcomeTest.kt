package dugsolutions.leaf.v35.player.decision.baseline.projection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class C03CultivationPipOutcomeTest {
    @Test fun eachPhysicalDieHasExactArithmeticExpectedPips() {
        mapOf(4 to 2.5, 6 to 3.5, 8 to 4.5, 10 to 5.5, 12 to 6.5, 20 to 10.5)
            .forEach { (sides, expected) -> assertEquals(expected, CultivationPipOutcome.expectedNewDie(sides)) }
    }

    @Test fun unavailableDrawHasNoProjectedPips() {
        assertNull(CultivationPipOutcome.nextDraw(emptyList(), emptyList()))
        assertEquals(0.0, CultivationPipOutcome.consecutiveDraws(emptyList(), emptyList(), 3))
    }

    @Test fun drawsRecycleDiscardOnlyOnceAndNeverInventDice() {
        assertEquals(3.5, CultivationPipOutcome.nextDraw(emptyList(), listOf(6)))
        assertEquals(7.0, CultivationPipOutcome.consecutiveDraws(listOf(4), listOf(8), 4))
        assertEquals(2.5, CultivationPipOutcome.nextDraw(listOf(4), listOf(20)))
    }

    @Test fun raisesAreCappedAndExistingFaceGainIsIncremental() {
        assertEquals(1, CultivationPipOutcome.raisedExistingDie(6, 5, 4))
        assertEquals(0, CultivationPipOutcome.raisedExistingDie(12, 12, 20))
        assertEquals(4, CultivationPipOutcome.raisedExistingDie(20, 10, 4))
        assertEquals(2, CultivationPipOutcome.changedExistingDie(5, 7, 8))
        assertEquals(-2, CultivationPipOutcome.changedExistingDie(7, 5, 8))
    }

    @Test fun invalidPhysicalFacesAndSizesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { CultivationPipOutcome.expectedNewDie(7) }
        assertThrows(IllegalArgumentException::class.java) { CultivationPipOutcome.changedExistingDie(6, 7, 6) }
    }
}
