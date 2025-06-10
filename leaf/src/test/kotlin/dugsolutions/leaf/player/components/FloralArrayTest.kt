package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCardIDs
import dugsolutions.leaf.cards.di.GameCardIDsFactory
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
    private lateinit var floralBonusCount: FloralBonusCount
    private lateinit var floralArray: FloralArray

    @BeforeEach
    fun setup() {
        cardManager = mockk(relaxed = true)
        gameCardIDsFactory = mockk(relaxed = true)
        gameCardIDs = mockk(relaxed = true)
        floralBonusCount = mockk(relaxed = true)
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
    fun clear_whenCalled_clearsStack() {
        // Arrange

        // Act
        floralArray.clear()

        // Assert
        verify { gameCardIDs.clear() }
    }
} 
