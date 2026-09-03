package dugsolutions.leaf.v35.tokens

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrittersTest {

    @Test
    fun constructor_withCritters_storesThemInOrder() {
        // Arrange
        val source = listOf(
            Critter.BEE,
            Critter.WORM,
            Critter.BOOSTED_WORM
        )

        // Act
        val result = Critters(source)

        // Assert
        assertEquals(source, result.all)
        assertEquals(3, result.size)
        assertTrue(result.isNotEmpty)
        assertFalse(result.isEmpty)
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCritters() {
        // Arrange
        val source = mutableListOf(
            Critter.BEE,
            Critter.WORM
        )
        val result = Critters(source)

        // Act
        source.clear()

        // Assert
        assertEquals(
            listOf(Critter.BEE, Critter.WORM),
            result.all
        )
    }

    @Test
    fun iterator_returnsCrittersInOrder() {
        // Arrange
        val expected = listOf(
            Critter.WORM,
            Critter.BEE,
            Critter.BOOSTED_BEE
        )
        val critters = Critters(expected)

        // Act
        val result = critters.toList()

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun add_addsCritterAndReturnsSameCollection() {
        // Arrange
        val critters = Critters()

        // Act
        val returned = critters.add(Critter.BEE)

        // Assert
        assertTrue(returned === critters)
        assertEquals(listOf(Critter.BEE), critters.all)
    }

    @Test
    fun count_returnsNumberOfMatchingCritters() {
        // Arrange
        val critters = Critters(
            listOf(
                Critter.BEE,
                Critter.WORM,
                Critter.BEE,
                Critter.BOOSTED_BEE
            )
        )

        // Act / Assert
        assertEquals(2, critters.count(Critter.BEE))
        assertEquals(1, critters.count(Critter.WORM))
        assertEquals(1, critters.count(Critter.BOOSTED_BEE))
        assertEquals(0, critters.count(Critter.BOOSTED_WORM))
    }

    @Test
    fun set_replacesExistingCountForThatCritter() {
        // Arrange
        val critters = Critters(
            listOf(
                Critter.BEE,
                Critter.BEE,
                Critter.WORM
            )
        )

        // Act
        val returned = critters.set(Critter.BEE, 3)

        // Assert
        assertTrue(returned === critters)
        assertEquals(3, critters.count(Critter.BEE))
        assertEquals(1, critters.count(Critter.WORM))
        assertEquals(4, critters.size)
    }

    @Test
    fun set_toZero_removesAllMatchingCritters() {
        // Arrange
        val critters = Critters(
            listOf(
                Critter.WORM,
                Critter.BEE,
                Critter.WORM
            )
        )

        // Act
        critters.set(Critter.WORM, 0)

        // Assert
        assertEquals(0, critters.count(Critter.WORM))
        assertEquals(listOf(Critter.BEE), critters.all)
    }

    @Test
    fun set_whenAmountNegative_throws() {
        // Arrange
        val critters = Critters()

        // Act / Assert
        assertFailsWith<IllegalArgumentException> {
            critters.set(Critter.BEE, -1)
        }
    }

    @Test
    fun remove_removesOneMatchingCritter() {
        // Arrange
        val critters = Critters(
            listOf(
                Critter.BEE,
                Critter.BEE,
                Critter.WORM
            )
        )

        // Act
        val result = critters.remove(Critter.BEE)

        // Assert
        assertTrue(result)
        assertEquals(1, critters.count(Critter.BEE))
        assertEquals(2, critters.size)
    }

    @Test
    fun remove_whenCritterMissing_returnsFalse() {
        // Arrange
        val critters = Critters(listOf(Critter.WORM))

        // Act
        val result = critters.remove(Critter.BEE)

        // Assert
        assertFalse(result)
        assertEquals(listOf(Critter.WORM), critters.all)
    }

    @Test
    fun replace_replacesAllMatchesAndReturnsNumberReplaced() {
        // Arrange
        val critters = Critters(
            listOf(
                Critter.WORM,
                Critter.BEE,
                Critter.WORM,
                Critter.WORM
            )
        )

        // Act
        val result = critters.replace(
            Critter.WORM,
            Critter.BOOSTED_WORM
        )

        // Assert
        assertEquals(3, result)
        assertEquals(0, critters.count(Critter.WORM))
        assertEquals(3, critters.count(Critter.BOOSTED_WORM))
        assertEquals(1, critters.count(Critter.BEE))
    }

    @Test
    fun clear_removesAllCritters() {
        // Arrange
        val critters = Critters(
            listOf(Critter.BEE, Critter.WORM)
        )

        // Act
        critters.clear()

        // Assert
        assertEquals(0, critters.size)
        assertTrue(critters.isEmpty)
        assertFalse(critters.isNotEmpty)
        assertEquals(emptyList(), critters.all)
    }
}
