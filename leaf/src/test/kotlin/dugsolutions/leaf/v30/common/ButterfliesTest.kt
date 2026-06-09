package dugsolutions.leaf.v30.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ButterfliesTest {

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalButterflies() {
        // Arrange
        val incoming = mutableListOf(Butterfly.GREEN, Butterfly.YELLOW)
        val butterflies = Butterflies(incoming)

        // Act
        incoming.clear()

        // Assert
        assertEquals(listOf(Butterfly.GREEN, Butterfly.YELLOW), butterflies.all)
    }

    @Test
    fun isEmpty_whenNewWithEmptyList_returnsTrue() {
        // Arrange
        val butterflies = Butterflies()

        // Assert
        assertTrue(butterflies.isEmpty)
        assertFalse(butterflies.isNotEmpty)
    }

    @Test
    fun add_appendsButterflyAndReturnsSameStack() {
        // Arrange
        val butterflies = Butterflies()

        // Act
        val result = butterflies.add(Butterfly.RED)

        // Assert
        assertEquals(butterflies, result)
        assertEquals(listOf(Butterfly.RED), butterflies.all)
    }

    @Test
    fun remove_whenButterflyExists_removesFirstMatch() {
        // Arrange
        val butterflies = Butterflies(listOf(Butterfly.GREEN, Butterfly.PURPLE, Butterfly.GREEN))

        // Act
        val result = butterflies.remove(Butterfly.GREEN)

        // Assert
        assertTrue(result)
        assertEquals(listOf(Butterfly.PURPLE, Butterfly.GREEN), butterflies.all)
    }

    @Test
    fun remove_whenButterflyDoesNotExist_returnsFalse() {
        // Arrange
        val butterflies = Butterflies(listOf(Butterfly.YELLOW))

        // Act
        val result = butterflies.remove(Butterfly.RED)

        // Assert
        assertFalse(result)
        assertEquals(listOf(Butterfly.YELLOW), butterflies.all)
    }

    @Test
    fun clear_removesAllButterflies() {
        // Arrange
        val butterflies = Butterflies(listOf(Butterfly.GREEN, Butterfly.YELLOW, Butterfly.RED, Butterfly.PURPLE))

        // Act
        butterflies.clear()

        // Assert
        assertTrue(butterflies.isEmpty)
        assertEquals(emptyList(), butterflies.all)
    }

    @Test
    fun iterator_returnsButterfliesInOrder() {
        // Arrange
        val expected = listOf(Butterfly.GREEN, Butterfly.YELLOW, Butterfly.RED, Butterfly.PURPLE)
        val butterflies = Butterflies(expected)

        // Act
        val result = butterflies.toList()

        // Assert
        assertEquals(expected, result)
    }
}
