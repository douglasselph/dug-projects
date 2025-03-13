package dugsolutions.leaf.market.domain

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.di.GameCardsFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameCardsUseCaseTest {

    private lateinit var mockGameCardsFactory: GameCardsFactory
    private lateinit var mockGameCard1: GameCard
    private lateinit var mockGameCard2: GameCard
    private lateinit var mockGameCard3: GameCard
    private lateinit var mockGameCards: GameCards

    private lateinit var SUT: GameCardsUseCase

    @BeforeEach
    fun setup() {
        // Create mocks
        mockGameCardsFactory = mockk(relaxed = true)
        mockGameCard1 = mockk(relaxed = true)
        mockGameCard2 = mockk(relaxed = true)
        mockGameCard3 = mockk(relaxed = true)
        mockGameCards = mockk(relaxed = true)

        // Initialize the use case
        SUT = GameCardsUseCase(mockGameCardsFactory)

        // Setup GameCardsFactory to return mockGameCards
        every { mockGameCardsFactory(any()) } returns mockGameCards
    }

    @Test
    fun invoke_withEmptyConfigs_returnsEmptyGameCards() {
        // Arrange
        val emptyConfigs = emptyList<MarketCardConfig>()
        val cardsSlot = slot<List<GameCard>>()
        
        every { mockGameCardsFactory(capture(cardsSlot)) } returns mockGameCards
        
        // Act
        val result = SUT(emptyConfigs)
        
        // Assert
        assertEquals(mockGameCards, result)
        assertTrue(cardsSlot.captured.isEmpty())
    }

    @Test
    fun invoke_withSingleConfigWithCountOne_returnsGameCardsWithSingleCard() {
        // Arrange
        val configs = listOf(
            MarketCardConfig(mockGameCard1, 1)
        )
        
        val cardsSlot = slot<List<GameCard>>()
        every { mockGameCardsFactory(capture(cardsSlot)) } returns mockGameCards
        
        // Act
        val result = SUT(configs)
        
        // Assert
        assertEquals(mockGameCards, result)
        assertEquals(1, cardsSlot.captured.size)
        assertEquals(mockGameCard1, cardsSlot.captured[0])
    }

    @Test
    fun invoke_withSingleConfigWithMultipleCount_returnsGameCardsWithDuplicatedCards() {
        // Arrange
        val configs = listOf(
            MarketCardConfig(mockGameCard1, 3)
        )
        
        val cardsSlot = slot<List<GameCard>>()
        every { mockGameCardsFactory(capture(cardsSlot)) } returns mockGameCards
        
        // Act
        val result = SUT(configs)
        
        // Assert
        assertEquals(mockGameCards, result)
        assertEquals(3, cardsSlot.captured.size)
        assertEquals(mockGameCard1, cardsSlot.captured[0])
        assertEquals(mockGameCard1, cardsSlot.captured[1])
        assertEquals(mockGameCard1, cardsSlot.captured[2])
    }

    @Test
    fun invoke_withMultipleConfigs_returnsGameCardsWithAllCards() {
        // Arrange
        val configs = listOf(
            MarketCardConfig(mockGameCard1, 2),
            MarketCardConfig(mockGameCard2, 1),
            MarketCardConfig(mockGameCard3, 3)
        )
        
        val cardsSlot = slot<List<GameCard>>()
        every { mockGameCardsFactory(capture(cardsSlot)) } returns mockGameCards
        
        // Act
        val result = SUT(configs)
        
        // Assert
        assertEquals(mockGameCards, result)
        assertEquals(6, cardsSlot.captured.size)
        
        // Verify cards are in the expected order based on configs
        val expectedCards = listOf(
            mockGameCard1, mockGameCard1,
            mockGameCard2,
            mockGameCard3, mockGameCard3, mockGameCard3
        )
        
        expectedCards.forEachIndexed { index, expectedCard ->
            assertEquals(expectedCard, cardsSlot.captured[index])
        }
    }

    @Test
    fun invoke_withZeroCount_skipsCard() {
        // Arrange
        val configs = listOf(
            MarketCardConfig(mockGameCard1, 0),
            MarketCardConfig(mockGameCard2, 2)
        )
        
        val cardsSlot = slot<List<GameCard>>()
        every { mockGameCardsFactory(capture(cardsSlot)) } returns mockGameCards
        
        // Act
        val result = SUT(configs)
        
        // Assert
        assertEquals(mockGameCards, result)
        assertEquals(2, cardsSlot.captured.size)
        assertEquals(mockGameCard2, cardsSlot.captured[0])
        assertEquals(mockGameCard2, cardsSlot.captured[1])
    }

    @Test
    fun invoke_callsGameCardsFactoryWithCorrectParameters() {
        // Arrange
        val configs = listOf(
            MarketCardConfig(mockGameCard1, 1),
            MarketCardConfig(mockGameCard2, 2)
        )
        
        // Act
        SUT(configs)
        
        // Assert
        val expectedCards = listOf(mockGameCard1, mockGameCard2, mockGameCard2)
        verify { mockGameCardsFactory(expectedCards) }
    }
} 
