package dugsolutions.leaf.v35.player.decision.baseline.scoring

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LegalChoiceCombinationsTest {
    @Test
    fun `between enumerates complete subsets in deterministic order`() {
        assertEquals(
            listOf(
                listOf(1), listOf(2), listOf(3),
                listOf(1, 2), listOf(1, 3), listOf(2, 3)
            ),
            LegalChoiceCombinations.between(listOf(1, 2, 3), 1, 2)
        )
    }

    @Test
    fun `between includes empty choice when minimum is zero`() {
        assertEquals(
            listOf(emptyList(), listOf("a"), listOf("b")),
            LegalChoiceCombinations.between(listOf("a", "b"), 0, 1)
        )
    }

    @Test
    fun `exactly enumerates only requested size`() {
        assertEquals(
            listOf(listOf("a", "b"), listOf("a", "c"), listOf("b", "c")),
            LegalChoiceCombinations.exactly(listOf("a", "b", "c"), 2)
        )
    }

    @Test
    fun `invalid bounds are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            LegalChoiceCombinations.between(listOf(1, 2), 0, 3)
        }
    }
}
