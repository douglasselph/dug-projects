package dugsolutions.leaf.v35.player.decision.random

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class StrategySeedStreamsTest {
    @Test
    fun purposeAndPlayerHaveIndependentDeterministicSequences() {
        val base = 123456789L
        fun stream(player: Int, purpose: StrategySeedStreams.Purpose) =
            StrategyRandomizer.create(StrategySeedStreams.seed(base, player, purpose))
        val a = stream(0, StrategySeedStreams.Purpose.DECISIONS)
        val b = stream(0, StrategySeedStreams.Purpose.SCORE_RANGES)
        val c = stream(1, StrategySeedStreams.Purpose.SCORE_RANGES)
        val bReference = stream(0, StrategySeedStreams.Purpose.SCORE_RANGES)
        val cReference = stream(1, StrategySeedStreams.Purpose.SCORE_RANGES)
        repeat(200) { a.nextInt(1000) } // Extra decisions cannot affect score range RNG.
        assertEquals(List(40) { bReference.nextInt(1000) }, List(40) { b.nextInt(1000) })
        assertEquals(List(40) { cReference.nextInt(1000) }, List(40) { c.nextInt(1000) })
        assertNotEquals(
            StrategySeedStreams.seed(base, 0, StrategySeedStreams.Purpose.SCORE_RANGES),
            StrategySeedStreams.seed(base, 1, StrategySeedStreams.Purpose.SCORE_RANGES)
        )
        assertNotEquals(
            StrategySeedStreams.seed(base, 0, StrategySeedStreams.Purpose.DECISIONS),
            StrategySeedStreams.seed(base, 0, StrategySeedStreams.Purpose.SCORE_RANGES)
        )
    }

    @Test
    fun retainsLegacyDecisionSeedAndUnseededMode() {
        assertEquals(42L xor 0x5DEECE66DL, StrategySeedStreams.seed(42, 0, StrategySeedStreams.Purpose.DECISIONS))
        assertNull(StrategySeedStreams.seed(null, 0, StrategySeedStreams.Purpose.SCORE_RANGES))
        assertFailsWith<IllegalArgumentException> { StrategySeedStreams.seed(1, -1, StrategySeedStreams.Purpose.DECISIONS) }
    }
}
