package dugsolutions.leaf.v35.tokens

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrittersTest {

    @Test
    fun constructor_withCritters_storesThemInOrder() {
        val source = listOf(
            Critter.BEE,
            Critter.WORM,
            Critter.WORM
        )

        val result = Critters(source)

        assertEquals(source, result.all)
        assertEquals(3, result.size)
        assertTrue(result.isNotEmpty)
        assertFalse(result.isEmpty)
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCritters() {
        val source = mutableListOf(
            Critter.BEE,
            Critter.WORM
        )
        val result = Critters(source)

        source.clear()

        assertEquals(
            listOf(Critter.BEE, Critter.WORM),
            result.all
        )
    }

    @Test
    fun iterator_returnsCrittersInOrder() {
        val expected = listOf(
            Critter.WORM,
            Critter.BEE,
            Critter.BEE
        )
        val critters = Critters(expected)

        assertEquals(expected, critters.toList())
    }

    @Test
    fun add_addsCritterAndReturnsSameCollection() {
        val critters = Critters()

        val returned = critters.add(Critter.BEE)

        assertTrue(returned === critters)
        assertEquals(listOf(Critter.BEE), critters.all)
    }

    @Test
    fun count_returnsNumberOfMatchingCritters() {
        val critters = Critters(
            listOf(
                Critter.BEE,
                Critter.WORM,
                Critter.BEE,
                Critter.BEE
            )
        )

        assertEquals(3, critters.count(Critter.BEE))
        assertEquals(1, critters.count(Critter.WORM))
    }

    @Test
    fun set_replacesExistingCountForThatCritter() {
        val critters = Critters(
            listOf(
                Critter.BEE,
                Critter.BEE,
                Critter.WORM
            )
        )

        val returned = critters.set(Critter.BEE, 3)

        assertTrue(returned === critters)
        assertEquals(3, critters.count(Critter.BEE))
        assertEquals(1, critters.count(Critter.WORM))
        assertEquals(4, critters.size)
    }

    @Test
    fun set_toZero_removesAllMatchingCritters() {
        val critters = Critters(
            listOf(
                Critter.WORM,
                Critter.BEE,
                Critter.WORM
            )
        )

        critters.set(Critter.WORM, 0)

        assertEquals(0, critters.count(Critter.WORM))
        assertEquals(listOf(Critter.BEE), critters.all)
    }

    @Test
    fun set_whenAmountNegative_throws() {
        val critters = Critters()

        assertFailsWith<IllegalArgumentException> {
            critters.set(Critter.BEE, -1)
        }
    }

    @Test
    fun remove_removesOneMatchingCritter() {
        val critters = Critters(
            listOf(
                Critter.BEE,
                Critter.BEE,
                Critter.WORM
            )
        )

        val result = critters.remove(Critter.BEE)

        assertTrue(result)
        assertEquals(1, critters.count(Critter.BEE))
        assertEquals(2, critters.size)
    }

    @Test
    fun remove_whenCritterMissing_returnsFalse() {
        val critters = Critters(listOf(Critter.WORM))

        val result = critters.remove(Critter.BEE)

        assertFalse(result)
        assertEquals(listOf(Critter.WORM), critters.all)
    }

    @Test
    fun replace_replacesAllMatchesAndReturnsNumberReplaced() {
        val critters = Critters(
            listOf(
                Critter.WORM,
                Critter.BEE,
                Critter.WORM,
                Critter.WORM
            )
        )

        val result = critters.replace(
            Critter.WORM,
            Critter.BEE
        )

        assertEquals(3, result)
        assertEquals(0, critters.count(Critter.WORM))
        assertEquals(4, critters.count(Critter.BEE))
    }

    @Test
    fun clear_removesAllCritters() {
        val critters = Critters(
            listOf(Critter.BEE, Critter.WORM)
        )

        critters.clear()

        assertEquals(0, critters.size)
        assertTrue(critters.isEmpty)
        assertFalse(critters.isNotEmpty)
        assertEquals(emptyList(), critters.all)
    }
}
