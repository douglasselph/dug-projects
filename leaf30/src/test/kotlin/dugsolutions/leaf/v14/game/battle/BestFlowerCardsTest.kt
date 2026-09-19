package dugsolutions.leaf.v14.game.battle

import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.cards.domain.MatchWith
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.decisions.DecisionDirector
import dugsolutions.leaf.v14.game.battle.BestFlowerCards
import dugsolutions.leaf.v14.game.battle.MatchingBloomCard
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BestFlowerCardsTest {

    private lateinit var mockMatchingBloomCard: MatchingBloomCard
    private lateinit var mockPlayer: Player
    private lateinit var mockDecisionDirector: DecisionDirector
    private lateinit var mockFlowerCard1: GameCard
    private lateinit var mockFlowerCard2: GameCard
    private lateinit var mockFlowerCard3: GameCard
    private lateinit var mockBloomCard1: GameCard
    private lateinit var mockBloomCard2: GameCard
    
    private lateinit var SUT: BestFlowerCards
    
    @BeforeEach
    fun setup() {
        mockMatchingBloomCard = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockDecisionDirector = mockk(relaxed = true)
        mockFlowerCard1 = mockk(relaxed = true)
        mockFlowerCard2 = mockk(relaxed = true)
        mockFlowerCard3 = mockk(relaxed = true)
        mockBloomCard1 = mockk(relaxed = true)
        mockBloomCard2 = mockk(relaxed = true)
        
        every { mockPlayer.decisionDirector } returns mockDecisionDirector
        every { mockFlowerCard1.id } returns 1
        every { mockFlowerCard2.id } returns 2
        every { mockFlowerCard3.id } returns 3
        every { mockBloomCard1.matchWith } returns MatchWith.Flower(1)
        every { mockBloomCard2.matchWith } returns MatchWith.Flower(2)
        
        SUT = BestFlowerCards(mockMatchingBloomCard)
    }
    
    @Test
    fun invoke_whenNoFlowerCards_returnsEmptyList() {
        // Arrange
        every { mockPlayer.floralCards } returns emptyList()
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun invoke_whenOneFlowerCard_returnsSingleCard() {
        // Arrange
        every { mockPlayer.floralCards } returns listOf(mockFlowerCard1)
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        assertEquals(listOf(mockFlowerCard1), result)
    }
    
    @Test
    fun invoke_whenTwoFlowerCards_returnsBothCards() {
        // Arrange
        every { mockPlayer.floralCards } returns listOf(mockFlowerCard1, mockFlowerCard2)
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        assertEquals(listOf(mockFlowerCard1, mockFlowerCard2), result)
    }
    
    @Test
    fun invoke_whenThreeFlowerCardsWithTie_usesDecisionDirector() {
        // Arrange
        // Two cards with frequency 2, one with frequency 1
        every { mockPlayer.floralCards } returns listOf(
            mockFlowerCard1, mockFlowerCard1,
            mockFlowerCard2, mockFlowerCard2,
            mockFlowerCard3
        )
        every { mockMatchingBloomCard(mockFlowerCard1) } returns mockBloomCard1
        every { mockMatchingBloomCard(mockFlowerCard2) } returns mockBloomCard2
        every { mockDecisionDirector.bestBloomCardAcquisition(listOf(mockBloomCard1, mockBloomCard2)) } returns mockBloomCard1
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        assertEquals(listOf(mockFlowerCard1, mockFlowerCard2), result)
    }
    
    @Test
    fun invoke_whenNoMatchingBloomCards_returnsFirstTwoCards() {
        // Arrange
        every { mockPlayer.floralCards } returns listOf(
            mockFlowerCard1, mockFlowerCard1,
            mockFlowerCard2, mockFlowerCard2,
            mockFlowerCard3
        )
        every { mockMatchingBloomCard(any()) } returns null
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        assertEquals(listOf(mockFlowerCard1, mockFlowerCard2), result)
    }
} 
