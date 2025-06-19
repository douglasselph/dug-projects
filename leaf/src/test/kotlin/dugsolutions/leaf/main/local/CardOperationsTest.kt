package dugsolutions.leaf.main.local

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.common.Commons.CARD_LIST
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.main.domain.CardInfoFaker
import dugsolutions.leaf.cards.CardRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CardOperationsTest {

    companion object {
        private const val CARD_NAME = "Test Card"
    }

    private val mockCardManager = mockk<CardManager>(relaxed = true)
    private val mockCardRegistry = mockk<CardRegistry>(relaxed = true)
    private val mockGameCardsFactory = mockk<GameCardsFactory>(relaxed = true)
    private val mockGameCards = mockk<GameCards>(relaxed = true)
    private val mockGameCard = mockk<GameCard>(relaxed = true)

    private val SUT = CardOperations(mockCardManager, mockCardRegistry, mockGameCardsFactory)

    @BeforeEach
    fun setup() {
        every { mockGameCardsFactory(any()) } returns mockGameCards
        every { mockGameCards.sortByCost() } returns mockGameCards
        every { mockCardManager.getCard(CARD_NAME) } returns mockGameCard
    }

    @Test
    fun setup_whenCalled_loadsCardsFromRegistry() {
        // Arrange
        // Act
        SUT.setup()

        // Assert
        verify { mockCardRegistry.loadFromCsv(CARD_LIST) }
        verify { mockCardManager.loadCards(mockCardRegistry) }
    }

    @Test
    fun getGameCards_whenTypeProvided_returnsSortedCards() {
        // Arrange
        val type = FlourishType.FLOWER
        val mockCards = listOf<GameCard>()
        every { mockCardManager.getCardsByType(type) } returns mockCards

        // Act
        val result = SUT.getGameCards(type)

        // Assert
        assertEquals(mockGameCards, result)
        verify { mockCardManager.getCardsByType(type) }
        verify { mockGameCardsFactory(mockCards) }
        verify { mockGameCards.sortByCost() }
    }

    @Test
    fun getCard_whenCardExists_returnsCard() {
        // Arrange
        val cardInfo = CardInfoFaker.create().copy(name = CARD_NAME)

        // Act
        val result = SUT.getCard(cardInfo)

        // Assert
        assertEquals(mockGameCard, result)
        verify { mockCardManager.getCard(CARD_NAME) }
    }

    @Test
    fun getCard_whenCardDoesNotExist_returnsNull() {
        // Arrange
        val cardInfo = CardInfoFaker.create().copy(name = "NonExistentCard")
        every { mockCardManager.getCard("NonExistentCard") } returns null

        // Act
        val result = SUT.getCard(cardInfo)

        // Assert
        assertNull(result)
        verify { mockCardManager.getCard("NonExistentCard") }
    }
} 
