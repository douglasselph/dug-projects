package dugsolutions.leaf.v35.player.decision.random

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class StrategyRandomizerTest {

    @Test
    fun sameSeed_reproducesStrategySequence() {
        val first = StrategyRandomizer.create(12345L)
        val second = StrategyRandomizer.create(12345L)

        val firstSequence = List(20) { first.nextInt(1000) }
        val secondSequence = List(20) { second.nextInt(1000) }

        assertEquals(firstSequence, secondSequence)
    }

    @Test
    fun differentSeeds_produceDifferentStrategySequences() {
        val first = StrategyRandomizer.create(12345L)
        val second = StrategyRandomizer.create(54321L)

        val firstSequence = List(20) { first.nextInt(1000) }
        val secondSequence = List(20) { second.nextInt(1000) }

        assertNotEquals(firstSequence, secondSequence)
    }

    @Test
    fun nextInt_rejectsNonPositiveBound() {
        assertFailsWith<IllegalArgumentException> {
            StrategyRandomizer.create(1L).nextInt(0)
        }
    }
}
