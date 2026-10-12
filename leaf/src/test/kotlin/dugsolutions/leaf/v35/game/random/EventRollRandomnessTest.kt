package dugsolutions.leaf.v35.game.random

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventRollRandomnessTest {
    @Test fun repeatableAndValid() {
        val a = (1..100).map { EventRollRandomness.faceFor(720002, 4, 1, 17, it, 6) }
        val b = (1..100).map { EventRollRandomness.faceFor(720002, 4, 1, 17, it, 6) }
        assertEquals(a, b)
        assertTrue(a.all { it in 1..6 })
        assertTrue(a.distinct().size > 1)
    }

    @Test fun unrelatedEventDoesNotChangeValue() {
        val chosen = EventRollRandomness.faceFor(720002, 4, 2, 17, 2, 4)
        repeat(100) { i -> EventRollRandomness.faceFor(720002, 4, 1, 9, i + 1, 20) }
        assertEquals(chosen, EventRollRandomness.faceFor(720002, 4, 2, 17, 2, 4))
    }

    @Test fun worksForAllDieSizes() {
        for (sides in listOf(4, 6, 8, 10, 12, 20)) {
            repeat(100) { i ->
                assertTrue(EventRollRandomness.faceFor(11, 3, 1, 1, i + 1, sides) in 1..sides)
            }
        }
    }

    @Test fun percentileIsIdenticalAcrossDieSizes() {
        val u = EventRollRandomness.percentileFor(720002, 4, 1, 17, 2)
        assertTrue(u >= 0.0 && u < 1.0)
        for (sides in listOf(4, 6, 8, 10, 12, 20)) {
            assertEquals((u * sides).toInt() + 1,
                EventRollRandomness.percentileFaceFor(720002, 4, 1, 17, 2, sides))
        }
    }

    @Test fun crossSizeCouplingIsMonotoneAndUniformInDistribution() {
        val samples = 20000
        val counts4 = IntArray(4)
        val counts6 = IntArray(6)
        repeat(samples) { i ->
            val f4 = EventRollRandomness.percentileFaceFor(9988, 3, 1, 17, i + 1, 4)
            val f6 = EventRollRandomness.percentileFaceFor(9988, 3, 1, 17, i + 1, 6)
            counts4[f4 - 1]++
            counts6[f6 - 1]++
            assertTrue(f6 >= f4)
        }
        assertTrue(counts4.all { it in 4400..5600 })
        assertTrue(counts6.all { it in 2900..3800 })
    }

    @Test fun unrelatedEventsDoNotChangeSharedPercentile() {
        val chosen = EventRollRandomness.percentileFaceFor(22, 4, 1, 17, 2, 6)
        repeat(1000) { i -> EventRollRandomness.percentileFaceFor(22, 4, 2, 9, i + 1, 4) }
        assertEquals(chosen, EventRollRandomness.percentileFaceFor(22, 4, 1, 17, 2, 6))
    }

}
