package dugsolutions.leaf.v35.player.decision.baseline.projection

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExpectedDrawProjectionTest {
    @Test fun `draws consume smallest supply first then refill from discard`() {
        assertEquals(listOf(4, 8, 6, 12), ExpectedDrawProjection.drawSides(listOf(8, 4), listOf(12, 6), 4))
    }

    @Test fun `not enough dice means fewer forecast draws`() {
        assertEquals(listOf(4), ExpectedDrawProjection.drawSides(listOf(4), emptyList(), 3))
    }

    @Test fun `unknown die predictions retain fractional arithmetic means`() {
        assertEquals(listOf(2.5, 3.5, 10.5),
            ExpectedDrawProjection.drawSides(listOf(20, 4, 6), emptyList(), 3)
                .map { (it + 1) / 2.0 })
    }

    @Test fun `forecasting cannot mutate source collections`() {
        val supply = mutableListOf(12, 4)
        val discard = mutableListOf(8, 6)
        ExpectedDrawProjection.drawSides(supply, discard, 4)
        assertEquals(listOf(12, 4), supply)
        assertEquals(listOf(8, 6), discard)
    }
    @Test fun `upgrade uses new mean against known original face`() {
        assertEquals(1.5, ExpectedDrawProjection.upgradeDelta(4, 2, 6))
        assertEquals(-0.5, ExpectedDrawProjection.upgradeDelta(4, 4, 6))
    }
}
