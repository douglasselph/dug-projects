package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.di.GameCardIDsFactory
import dugsolutions.leaf.cards.FakeCards
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloralArrayTest {

    private lateinit var cardManager: CardManager
    private lateinit var gameCardIDsFactory: GameCardIDsFactory
    private lateinit var gameCardIDs: GameCardIDs
    private lateinit var floralArray: FloralArray

    @BeforeEach
    fun setup() {
        cardManager = mockk(relaxed = true)
        gameCardIDsFactory = mockk(relaxed = true)
        gameCardIDs = mockk(relaxed = true)
        every { gameCardIDsFactory(any()) } returns gameCardIDs
        floralArray = FloralArray(cardManager, gameCardIDsFactory)
    }

    @Test
    fun cards_whenEmpty_returnsEmptyList() {
        // Arrange
        every { gameCardIDs.cardIds } returns emptyList()

        // Act
        val result = floralArray.cards

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun cards_whenHasCards_returnsAllCards() {
        // Arrange
        val flower1 = FakeCards.fakeFlower
        val flower2 = FakeCards.fakeFlower2
        val cardIds = listOf(flower1.id, flower2.id)

        every { gameCardIDs.cardIds } returns cardIds
        every { cardManager.getCard(flower1.id) } returns flower1
        every { cardManager.getCard(flower2.id) } returns flower2

        // Act
        val result = floralArray.cards

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.contains(flower1))
        assertTrue(result.contains(flower2))
    }

    @Test
    fun add_whenCalled_addsCardToStack() {
        // Arrange
        val flower = FakeCards.fakeFlower

        // Act
        floralArray.add(flower.id)

        // Assert
        verify { gameCardIDs.add(flower.id) }
    }

    @Test
    fun floralCount_whenNoMatchingCards_returnsZero() {
        // Arrange
        val flower = FakeCards.fakeFlower
        every { gameCardIDs.cardIds } returns emptyList()

        // Act
        val result = floralArray.floralCount(flower.id)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun floralCount_whenOneMatchingCard_returnsOne() {
        // Arrange
        val flower = FakeCards.fakeFlower
        every { gameCardIDs.cardIds } returns listOf(flower.id)

        // Act
        val result = floralArray.floralCount(flower.id)

        // Assert
        assertEquals(1, result)
    }

    @Test
    fun floralCount_whenTwoMatchingCards_returnsTwo() {
        // Arrange
        val flower = FakeCards.fakeFlower
        every { gameCardIDs.cardIds } returns listOf(flower.id, flower.id)

        // Act
        val result = floralArray.floralCount(flower.id)

        // Assert
        assertEquals(2, result)
    }

    @Test
    fun floralCount_whenOneMatchingAndTwoNonMatching_returnsTwo() {
        // Arrange
        val flower = FakeCards.fakeFlower
        val otherFlower = FakeCards.fakeFlower2
        every { gameCardIDs.cardIds } returns listOf(flower.id, otherFlower.id, otherFlower.id)

        // Act
        val result = floralArray.floralCount(flower.id)

        // Assert
        assertEquals(2, result) // 1 matching + (2 non-matching / 2) = 1 + 1 = 2
    }

    @Test
    fun floralCount_whenOneMatchingAndThreeNonMatching_returnsTwo() {
        // Arrange
        val flower = FakeCards.fakeFlower
        val otherFlower = FakeCards.fakeFlower2
        every { gameCardIDs.cardIds } returns listOf(flower.id, otherFlower.id, otherFlower.id, otherFlower.id)

        // Act
        val result = floralArray.floralCount(flower.id)

        // Assert
        assertEquals(2, result) // 1 matching + (3 non-matching / 2) = 1 + 1 = 2
    }

    @Test
    fun clear_whenCalled_clearsStack() {
        // Arrange

        // Act
        floralArray.clear()

        // Assert
        verify { gameCardIDs.clear() }
    }
} 
