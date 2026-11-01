package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** No mocked game actions: tests the exhaustive, deterministic cap gate. */
class EngagementSupportContinuationTest {
    private val continuation = EngagementSupportContinuation()

    @Test fun `a two Bee path is rejected with one remaining Support`() {
        val one = continuation.feasibleCumulativePairs(1, 4, 0) { bees, _ -> bees >= 2 }
        assertTrue(one.isEmpty())
        val two = continuation.feasibleCumulativePairs(2, 4, 0) { bees, _ -> bees >= 2 }
        assertEquals(listOf(2 to 0), two)
    }

    @Test fun `one Bee plus Butterfly synergy fits exactly two Supports`() {
        val one = continuation.feasibleCumulativePairs(1, 3, 3) { bees, butterflies ->
            bees > 0 && butterflies > 0
        }
        assertTrue(one.isEmpty())
        val two = continuation.feasibleCumulativePairs(2, 3, 3) { bees, butterflies ->
            bees > 0 && butterflies > 0
        }
        assertEquals(listOf(1 to 1), two)
    }

    @Test fun `no Support budget produces no cumulative continuation`() {
        val result = continuation.feasibleCumulativePairs(0, 10, 10) { _, _ -> true }
        assertTrue(result.isEmpty())
    }

    @Test fun `a one move path is preserved and no solution exceeds the cap`() {
        val candidates = continuation.feasibleCumulativePairs(2, 6, 6) { bees, butterflies ->
            bees + butterflies > 0
        }
        assertTrue(1 to 0 in candidates)
        assertTrue(0 to 1 in candidates)
        assertTrue(candidates.all { it.first + it.second <= 2 })
    }
}
