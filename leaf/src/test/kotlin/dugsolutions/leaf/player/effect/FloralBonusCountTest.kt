package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.FakeCards
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FloralBonusCountTest {

    companion object {
        private val bloomFlowerCard = FakeCards.fakeFlower
    }
    private val SUT = FloralBonusCount()

    @Test
    fun invoke_whenNoMatchingCards_returnsZero() {
        // Arrange
        val cardIds = emptyList<Int>()

        // Act
        val result = SUT(cardIds, bloomFlowerCard.id)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun invoke_whenOneMatchingCard_returnsOne() {
        // Arrange
        val cardIds = listOf(bloomFlowerCard.id)

        // Act
        val result = SUT(cardIds, bloomFlowerCard.id)

        // Assert
        assertEquals(1, result)
    }

    @Test
    fun invoke_whenTwoMatchingCards_returnsTwo() {
        // Arrange
        val cardIds = listOf(bloomFlowerCard.id, bloomFlowerCard.id)

        // Act
        val result = SUT(cardIds, bloomFlowerCard.id)

        // Assert
        assertEquals(2, result)
    }

    @Test
    fun invoke_whenTwoMatchingAnd4NonMatching_returnsFour() {
        // Arrange
        val otherFlower = FakeCards.fakeFlower2
        val otherFlower2 = FakeCards.fakeFlower3
        val cardIds = listOf(
            bloomFlowerCard.id,
            bloomFlowerCard.id,
            otherFlower.id,
            otherFlower.id,
            otherFlower.id,
            otherFlower2.id
        )

        // Act
        val result = SUT(cardIds, bloomFlowerCard.id)

        // Assert
        assertEquals(4, result)
    }

    @Test
    fun invoke_whenOneMatchingAnd3NonMatching_returnsThree() {
        // Arrange
        val otherFlower = FakeCards.fakeFlower2
        val cardIds = listOf(bloomFlowerCard.id, otherFlower.id, otherFlower.id, otherFlower.id)

        // Act
        val result = SUT(cardIds, bloomFlowerCard.id)

        // Assert
        assertEquals(3, result)
    }

    @Test
    fun invoke_whenOneMatchingAndTwoDifferentNonMatchingTypes_returnsTwo() {
        // Arrange
        val otherFlower1 = FakeCards.fakeFlower2
        val otherFlower2 = FakeCards.fakeFlower3
        val cardIds = listOf(bloomFlowerCard.id, otherFlower1.id, otherFlower2.id)

        // Act
        val result = SUT(cardIds, bloomFlowerCard.id)

        // Assert
        assertEquals(2, result) // 1 matching + 2 unique non-matching types = 3
    }

    @Test
    fun invoke_whenOneMatchingAndOneNonMatchingTypes_returnsTwo() {
        // Arrange
        val otherFlower1 = FakeCards.fakeFlower2
        val cardIds = listOf(bloomFlowerCard.id, otherFlower1.id)

        // Act
        val result = SUT(cardIds, bloomFlowerCard.id)

        // Assert
        assertEquals(2, result) // 1 matching + 2 unique non-matching types = 3
    }
} 
