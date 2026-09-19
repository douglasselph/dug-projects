package dugsolutions.leaf.v30.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrittersTest {

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCritters() {
        // Arrange
        val incoming = mutableListOf(Critter.BEE, Critter.WORM)
        val critters = Critters(incoming)

        // Act
        incoming.clear()

        // Assert
        assertEquals(listOf(Critter.BEE, Critter.WORM), critters.all)
    }

    @Test
    fun isEmpty_whenNewWithEmptyList_returnsTrue() {
        // Arrange
        val critters = Critters()

        // Assert
        assertTrue(critters.isEmpty)
        assertFalse(critters.isNotEmpty)
    }

    @Test
    fun add_appendsCritterAndReturnsSameStack() {
        // Arrange
        val critters = Critters()

        // Act
        val result = critters.add(Critter.BEE)

        // Assert
        assertEquals(critters, result)
        assertEquals(listOf(Critter.BEE), critters.all)
    }


    @Test
    fun count_returnsNumberOfMatchingCritters() {
        // Arrange
        val critters = Critters(listOf(Critter.BEE, Critter.WORM, Critter.BEE))

        // Assert
        assertEquals(2, critters.count(Critter.BEE))
        assertEquals(1, critters.count(Critter.WORM))
    }

    @Test
    fun set_replacesOnlyMatchingCritterCount() {
        // Arrange
        val critters = Critters(listOf(Critter.BEE, Critter.WORM, Critter.BEE))

        // Act
        val result = critters.set(Critter.BEE, 1)

        // Assert
        assertEquals(critters, result)
        assertEquals(1, critters.count(Critter.BEE))
        assertEquals(1, critters.count(Critter.WORM))
        assertEquals(listOf(Critter.WORM, Critter.BEE), critters.all)
    }

    @Test
    fun set_withNegativeAmount_throwsException() {
        // Arrange
        val critters = Critters()

        // Act & Assert
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            critters.set(Critter.BEE, -1)
        }
    }

    @Test
    fun remove_whenCritterExists_removesFirstMatch() {
        // Arrange
        val critters = Critters(listOf(Critter.BEE, Critter.WORM, Critter.BEE))

        // Act
        val result = critters.remove(Critter.BEE)

        // Assert
        assertTrue(result)
        assertEquals(listOf(Critter.WORM, Critter.BEE), critters.all)
    }

    @Test
    fun remove_whenCritterDoesNotExist_returnsFalse() {
        // Arrange
        val critters = Critters(listOf(Critter.BEE))

        // Act
        val result = critters.remove(Critter.WORM)

        // Assert
        assertFalse(result)
        assertEquals(listOf(Critter.BEE), critters.all)
    }

    @Test
    fun clear_removesAllCritters() {
        // Arrange
        val critters = Critters(listOf(Critter.BEE, Critter.WORM))

        // Act
        critters.clear()

        // Assert
        assertTrue(critters.isEmpty)
        assertEquals(emptyList(), critters.all)
    }

    @Test
    fun iterator_returnsCrittersInOrder() {
        // Arrange
        val expected = listOf(Critter.BEE, Critter.WORM)
        val critters = Critters(expected)

        // Act
        val result = critters.toList()

        // Assert
        assertEquals(expected, result)
    }
}
