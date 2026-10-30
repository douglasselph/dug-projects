package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.player.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattlePacingStatisticsTest {
    @Test fun `Wilson interval works for extreme frequencies and zero trials`() {
        assertTrue(wilson95(0, 0).first.isNaN())
        val zero = wilson95(0, 100)
        val all = wilson95(100, 100)
        assertEquals(0.0, zero.first, 1e-12)
        assertTrue(zero.second < 0.04)
        assertTrue(all.first > 0.96)
        assertEquals(1.0, all.second, 1e-12)
    }

    @Test fun `paired sample CI counts each Battle once`() {
        val stats = RunningMoments()
        listOf(2.0, -1.0, 3.0, 0.0).forEach(stats::add)
        assertEquals(4, stats.count)
        assertEquals(1.0, stats.mean, 1e-12)
        val ci = stats.interval95()
        assertTrue(ci.first < 1.0 && ci.second > 1.0)
        assertTrue(RunningMoments().interval95().first.isNaN())
    }

    @Test fun `pre last actor winners use actual all tied rule`() {
        val a = PlayerId(1)
        val b = PlayerId(2)
        val c = PlayerId(3)
        assertTrue(projectedWinners(mapOf(a to 7, b to 7)).isEmpty())
        assertEquals(setOf(a, b), projectedWinners(mapOf(a to 7, b to 7, c to 3)))
        assertEquals(setOf(c), projectedWinners(mapOf(a to 1, b to 5, c to 6)))
        assertTrue(projectedWinners(emptyMap()).isEmpty())
    }
}
