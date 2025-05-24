package dugsolutions.leaf.game.battle

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchingBloomCardTest {

    private lateinit var mockCardManager: CardManager
    private lateinit var mockFlowerCard: GameCard
    private lateinit var mockBloomCard: GameCard
    private lateinit var mockNonMatchingBloomCard: GameCard
    
    private lateinit var SUT: MatchingBloomCard
    
    @BeforeEach
    fun setup() {
        mockCardManager = mockk(relaxed = true)
        mockFlowerCard = mockk(relaxed = true)
        mockBloomCard = mockk(relaxed = true)
        mockNonMatchingBloomCard = mockk(relaxed = true)
        
        every { mockFlowerCard.id } returns 1
        every { mockBloomCard.type } returns FlourishType.BLOOM
        every { mockNonMatchingBloomCard.type } returns FlourishType.BLOOM
        every { mockBloomCard.matchWith } returns MatchWith.Flower(1)
        every { mockNonMatchingBloomCard.matchWith } returns MatchWith.Flower(2)
        
        SUT = MatchingBloomCard(mockCardManager)
    }
    
    @Test
    fun invoke_whenMatchingBloomExists_returnsBloomCard() {
        // Arrange
        every { mockCardManager.getCardsByType(FlourishType.BLOOM) } returns listOf(mockBloomCard, mockNonMatchingBloomCard)
        
        // Act
        val result = SUT(mockFlowerCard)
        
        // Assert
        assertEquals(mockBloomCard, result)
    }
    
    @Test
    fun invoke_whenNoMatchingBloomExists_returnsNull() {
        // Arrange
        every { mockCardManager.getCardsByType(FlourishType.BLOOM) } returns listOf(mockNonMatchingBloomCard)
        
        // Act
        val result = SUT(mockFlowerCard)
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun invoke_whenNoBloomCardsExist_returnsNull() {
        // Arrange
        every { mockCardManager.getCardsByType(FlourishType.BLOOM) } returns emptyList()
        
        // Act
        val result = SUT(mockFlowerCard)
        
        // Assert
        assertNull(result)
    }
} 
