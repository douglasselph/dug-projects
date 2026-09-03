package dugsolutions.leaf.v35.tokens

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ButterfliesTest {

    @Test
    fun constructor_withButterflies_storesThemInOrderAndFaceUp() {
        // Arrange
        val source = listOf(
            Butterfly.GREEN,
            Butterfly.YELLOW,
            Butterfly.RED
        )

        // Act
        val result = Butterflies(source)

        // Assert
        assertEquals(source, result.all)
        assertEquals(3, result.size)
        assertTrue(result.isNotEmpty)
        assertFalse(result.isEmpty)
        source.forEach { butterfly ->
            assertTrue(result.isFaceUp(butterfly))
            assertFalse(result.isFaceDown(butterfly))
        }
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalButterflies() {
        // Arrange
        val source = mutableListOf(
            Butterfly.GREEN,
            Butterfly.YELLOW
        )
        val result = Butterflies(source)

        // Act
        source.clear()

        // Assert
        assertEquals(
            listOf(Butterfly.GREEN, Butterfly.YELLOW),
            result.all
        )
    }

    @Test
    fun iterator_returnsButterfliesInOrder() {
        // Arrange
        val expected = listOf(
            Butterfly.PURPLE,
            Butterfly.GREEN,
            Butterfly.RED
        )
        val butterflies = Butterflies(expected)

        // Act
        val result = butterflies.toList()

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun add_addsButterflyFaceUpAndReturnsSameCollection() {
        // Arrange
        val butterflies = Butterflies()

        // Act
        val returned = butterflies.add(Butterfly.PURPLE)

        // Assert
        assertTrue(returned === butterflies)
        assertEquals(listOf(Butterfly.PURPLE), butterflies.all)
        assertTrue(butterflies.isFaceUp(Butterfly.PURPLE))
    }

    @Test
    fun faceDown_whenButterflyExists_changesItsState() {
        // Arrange
        val butterflies = Butterflies(listOf(Butterfly.GREEN))

        // Act
        val result = butterflies.faceDown(Butterfly.GREEN)

        // Assert
        assertTrue(result)
        assertTrue(butterflies.isFaceDown(Butterfly.GREEN))
        assertFalse(butterflies.isFaceUp(Butterfly.GREEN))
    }

    @Test
    fun faceUp_afterFaceDown_restoresItsState() {
        // Arrange
        val butterflies = Butterflies(listOf(Butterfly.GREEN))
        butterflies.faceDown(Butterfly.GREEN)

        // Act
        val result = butterflies.faceUp(Butterfly.GREEN)

        // Assert
        assertTrue(result)
        assertTrue(butterflies.isFaceUp(Butterfly.GREEN))
        assertFalse(butterflies.isFaceDown(Butterfly.GREEN))
    }

    @Test
    fun faceUpAndFaceDown_whenButterflyMissing_returnFalse() {
        // Arrange
        val butterflies = Butterflies()

        // Act / Assert
        assertFalse(butterflies.faceUp(Butterfly.RED))
        assertFalse(butterflies.faceDown(Butterfly.RED))
        assertFalse(butterflies.isFaceUp(Butterfly.RED))
        assertFalse(butterflies.isFaceDown(Butterfly.RED))
    }

    @Test
    fun remove_whenButterflyExists_removesItAndItsState() {
        // Arrange
        val butterflies = Butterflies(
            listOf(Butterfly.GREEN, Butterfly.RED)
        )
        butterflies.faceDown(Butterfly.GREEN)

        // Act
        val result = butterflies.remove(Butterfly.GREEN)

        // Assert
        assertTrue(result)
        assertEquals(listOf(Butterfly.RED), butterflies.all)
        assertFalse(butterflies.isFaceUp(Butterfly.GREEN))
        assertFalse(butterflies.isFaceDown(Butterfly.GREEN))
    }

    @Test
    fun remove_whenButterflyMissing_returnsFalse() {
        // Arrange
        val butterflies = Butterflies(listOf(Butterfly.GREEN))

        // Act
        val result = butterflies.remove(Butterfly.YELLOW)

        // Assert
        assertFalse(result)
        assertEquals(listOf(Butterfly.GREEN), butterflies.all)
    }

    @Test
    fun clear_removesAllButterfliesAndFaceState() {
        // Arrange
        val butterflies = Butterflies(
            listOf(Butterfly.GREEN, Butterfly.YELLOW)
        )
        butterflies.faceDown(Butterfly.GREEN)

        // Act
        butterflies.clear()

        // Assert
        assertEquals(0, butterflies.size)
        assertTrue(butterflies.isEmpty)
        assertFalse(butterflies.isNotEmpty)
        assertEquals(emptyList(), butterflies.all)
        assertFalse(butterflies.isFaceDown(Butterfly.GREEN))
        assertFalse(butterflies.isFaceUp(Butterfly.YELLOW))
    }
}
