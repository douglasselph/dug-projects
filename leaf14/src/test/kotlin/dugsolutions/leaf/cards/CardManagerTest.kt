package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.random.RandomizerTD
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CardManagerTest {

    companion object {
        // Card IDs (as integers since CardID is a typealias for Int)
        private const val NONEXISTENT_CARD_ID = 999
    }

    private lateinit var gameCardsFactory: GameCardsFactory
    private lateinit var randomizer: Randomizer
    private lateinit var SUT: CardManager

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        randomizer = RandomizerTD()
        gameCardsFactory = GameCardsFactory(randomizer = randomizer)

        SUT = CardManager(gameCardsFactory)
        SUT.loadCards(FakeCards.ALL_CARDS)
    }

    @Test
    fun getCard_byId_whenCardExists_returnsCard() {
        // Arrange - Card exists in registry
        
        // Act
        val result = SUT.getCard(FakeCards.bloomCard.id)
        
        // Assert
        assertEquals(FakeCards.bloomCard, result)
    }

    @Test
    fun getCard_byId_whenCardDoesNotExist_returnsNull() {
        // Arrange - Card doesn't exist in registry
        
        // Act
        val result = SUT.getCard(NONEXISTENT_CARD_ID)
        
        // Assert
        assertNull(result)
    }

    @Test
    fun getCard_byName_whenCardExists_returnsCard() {
        // Arrange - Card exists in registry
        
        // Act
        val result = SUT.getCard(FakeCards.bloomCard.name)
        
        // Assert
        assertEquals(FakeCards.bloomCard, result)
    }

    @Test
    fun getCard_byName_whenCardDoesNotExist_returnsNull() {
        // Arrange - Card doesn't exist in registry
        
        // Act
        val result = SUT.getCard("Nonexistent Card")
        
        // Assert
        assertNull(result)
    }

    @Test
    fun getCardsByIds_whenAllCardsExist_returnsAllCards() {
        // Arrange
        val cardIds = listOf(FakeCards.bloomCard.id, FakeCards.seedlingCard.id)
        
        // Act
        val result = SUT.getCardsByIds(cardIds)
        
        // Assert
        assertEquals(2, result.size)
        assertEquals(FakeCards.bloomCard, result[0])
        assertEquals(FakeCards.seedlingCard, result[1])
    }

    @Test
    fun getCardsByIds_whenSomeCardsDontExist_returnsOnlyExistingCards() {
        // Arrange
        val cardIds = listOf(FakeCards.bloomCard.id, NONEXISTENT_CARD_ID, FakeCards.rootCard.id)
        
        // Act
        val result = SUT.getCardsByIds(cardIds)
        
        // Assert
        assertEquals(2, result.size)
        assertEquals(FakeCards.bloomCard, result[0])
        assertEquals(FakeCards.rootCard, result[1])
    }

    @Test
    fun getGameCardsByType_whenTypeExists_returnsGameCards() {
        // Arrange
        val expectedCards = FakeCards.ALL_SEEDLINGS // All seedlings
        
        // Act
        val result = SUT.getGameCardsByType(FlourishType.NONE)
        
        // Assert
        assertEquals(expectedCards, result.cards)
    }

    @Test
    fun getGameCardsByType_whenTypeDoesNotExist_returnsEmptyGameCards() {
        // Arrange
        val expectedCards = emptyList<GameCard>()
        
        // Act
        val result = SUT.getGameCardsByType(FlourishType.CANOPY)
        
        // Assert
        assertEquals(expectedCards, result.cards)
    }

    @Test
    fun getCardsByType_whenTypeExists_returnsCards() {
        // Arrange
        val expectedCards = FakeCards.ALL_SEEDLINGS // All seedlings
        
        // Act
        val result = SUT.getCardsByType(FlourishType.NONE)
        
        // Assert
        assertEquals(expectedCards, result)
    }

    @Test
    fun getCardsByType_whenTypeDoesNotExist_returnsEmptyList() {
        // Arrange
        val expectedCards = emptyList<GameCard>()
        
        // Act
        val result = SUT.getCardsByType(FlourishType.CANOPY)
        
        // Assert
        assertEquals(expectedCards, result)
    }
}
