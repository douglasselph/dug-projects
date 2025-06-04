package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.FakeCards
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FloralCountTest {

    private val SUT = FloralCount()

    @Test
    fun invoke_whenNoMatchingCards_returnsZero() {
        // Arrange
        val flower = FakeCards.fakeFlower
        val cardIds = emptyList<Int>()

        // Act
        val result = SUT(cardIds, flower.id)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun invoke_whenOneMatchingCard_returnsOne() {
        // Arrange
        val flower = FakeCards.fakeFlower
        val cardIds = listOf(flower.id)

        // Act
        val result = SUT(cardIds, flower.id)

        // Assert
        assertEquals(1, result)
    }

    @Test
    fun invoke_whenTwoMatchingCards_returnsTwo() {
        // Arrange
        val flower = FakeCards.fakeFlower
        val cardIds = listOf(flower.id, flower.id)

        // Act
        val result = SUT(cardIds, flower.id)

        // Assert
        assertEquals(2, result)
    }

    @Test
    fun invoke_whenOneMatchingAndTwoNonMatching_returnsTwo() {
        // Arrange
        val flower = FakeCards.fakeFlower
        val otherFlower = FakeCards.fakeFlower2
        val cardIds = listOf(flower.id, otherFlower.id, otherFlower.id)

        // Act
        val result = SUT(cardIds, flower.id)

        // Assert
        assertEquals(2, result) // 1 matching + (2 non-matching / 2) = 1 + 1 = 2
    }

    @Test
    fun invoke_whenOneMatchingAndThreeNonMatching_returnsTwo() {
        // Arrange
        val flower = FakeCards.fakeFlower
        val otherFlower = FakeCards.fakeFlower2
        val cardIds = listOf(flower.id, otherFlower.id, otherFlower.id, otherFlower.id)

        // Act
        val result = SUT(cardIds, flower.id)

        // Assert
        assertEquals(2, result) // 1 matching + (3 non-matching / 2) = 1 + 1 = 2
    }
} 