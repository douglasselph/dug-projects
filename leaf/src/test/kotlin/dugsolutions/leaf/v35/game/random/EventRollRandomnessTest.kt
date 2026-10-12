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
}
