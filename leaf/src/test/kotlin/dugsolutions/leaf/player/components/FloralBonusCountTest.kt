package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.FakeCards
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FloralBonusCountTest {

    private val SUT = FloralBonusCount()

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
    fun invoke_whenOneMatchingAndMultipleSameTypeNonMatching_returnsTwo() {
        // Arrange
        val flower = FakeCards.fakeFlower
        val otherFlower = FakeCards.fakeFlower2
        val cardIds = listOf(flower.id, otherFlower.id, otherFlower.id, otherFlower.id)

        // Act
        val result = SUT(cardIds, flower.id)

        // Assert
        assertEquals(2, result) // 1 matching + 1 unique non-matching type = 2
    }

    @Test
    fun invoke_whenOneMatchingAndTwoDifferentNonMatchingTypes_returnsThree() {
        // Arrange
        val flower = FakeCards.fakeFlower
        val otherFlower1 = FakeCards.fakeFlower2
        val otherFlower2 = FakeCards.fakeFlower3
        val cardIds = listOf(flower.id, otherFlower1.id, otherFlower2.id)

        // Act
        val result = SUT(cardIds, flower.id)

        // Assert
        assertEquals(3, result) // 1 matching + 2 unique non-matching types = 3
    }
} 
