package dugsolutions.leaf.simulation.v35.replay

import dugsolutions.leaf.v35.game.random.EventRollRandomness
import dugsolutions.leaf.v35.game.random.ReplayRollEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayEquivalenceDiagnosticsTest {
    private fun event(id: Long, sides: Int, ordinal: Int = 1): ReplayRollEvent {
        val u = EventRollRandomness.percentileFor(720002, 4, 1, id, 2)
        return ReplayRollEvent(ordinal,4,1,id,2,sides,(u*sides).toInt()+1,u)
    }

    @Test fun matchingCrossSizeEventsSharePercentile() {
        val comparison = ReplayEquivalenceDiagnostics.compareRolls(listOf(event(17,4)), listOf(event(17,6)))
        assertEquals(1,comparison.matchedCrossSize)
        assertEquals(0,comparison.unexpectedMismatches)
    }

    @Test fun extraEventsDoNotShiftOthers() {
        val comparison = ReplayEquivalenceDiagnostics.compareRolls(listOf(event(17,4)),
            listOf(event(99,6),event(17,4,2)))
        assertEquals(1,comparison.matchedSameSize)
        assertEquals(1,comparison.comparisonOnly)
        assertEquals(0,comparison.unexpectedMismatches)
    }

    @Test fun changesToIdenticalKeyAreReported() {
        val original = event(17,4)
        val comparison = ReplayEquivalenceDiagnostics.compareRolls(listOf(original),
            listOf(original.copy(face=if(original.face==1) 2 else 1)))
        assertEquals(1,comparison.unexpectedMismatches)
        assertEquals(original.key, comparison.firstUnexpectedKey)
    }
}
