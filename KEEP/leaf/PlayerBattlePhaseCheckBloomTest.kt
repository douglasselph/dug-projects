package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerBattlePhaseCheckBloomTest {
    
    private lateinit var mockGrove: Grove
    private lateinit var mockPlayer: Player
    private lateinit var mockGameCardIds1: GameCardIDs
    private lateinit var mockGameCardIds2: GameCardIDs
    private lateinit var mockGameCardIds3: GameCardIDs

    private lateinit var SUT: PlayerBattlePhaseCheckBloom

    @BeforeEach
    fun setup() {
        mockGrove = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockGameCardIds1 = mockk(relaxed = true)
        mockGameCardIds2 = mockk(relaxed = true)
        mockGameCardIds3 = mockk(relaxed = true)
        
        SUT = PlayerBattlePhaseCheckBloom(mockGrove)
    }
    
    // isReady tests
    
    @Test
    fun isReady_whenPlayerHasTwoOrMoreBloomCards_returnsTrue() {
        // Arrange
        every { mockPlayer.bloomCount } returns 2
        
        // Act
        val result = SUT.isReady(mockPlayer)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun isReady_whenPlayerHasMoreThanTwoBloomCards_returnsTrue() {
        // Arrange
        every { mockPlayer.bloomCount } returns 3
        
        // Act
        val result = SUT.isReady(mockPlayer)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun isReady_whenPlayerHasLessThanTwoBloomCards_returnsFalse() {
        // Arrange
        every { mockPlayer.bloomCount } returns 1
        
        // Act
        val result = SUT.isReady(mockPlayer)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun isReady_whenPlayerHasNoBloomCards_returnsFalse() {
        // Arrange
        every { mockPlayer.bloomCount } returns 0
        
        // Act
        val result = SUT.isReady(mockPlayer)
        
        // Assert
        assertFalse(result)
    }
    
    // giftTo tests
    
    @Test
    fun giftTo_whenPlayerIsNotReadyAndBloom1StackHasCards_givesCardFromBloom1() {
        // Arrange
        val cardId = 101
        
        every { mockPlayer.bloomCount } returns 1
        every { mockGameCardIds1.isEmpty() } returns false
        every { mockGameCardIds1.cardIds } returns listOf(cardId)
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_1) } returns mockGameCardIds1
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify { mockGrove.removeCard(cardId) }
        verify { mockPlayer.addCardToCompost(cardId) }
        
        // Verify we didn't check stacks 2 and 3
        verify(exactly = 0) { mockGrove.getCardsFor(MarketStackID.BLOOM_2) }
        verify(exactly = 0) { mockGrove.getCardsFor(MarketStackID.BLOOM_3) }
    }
    
    @Test
    fun giftTo_whenPlayerIsNotReadyAndBloom1StackIsEmptyButBloom2HasCards_givesCardFromBloom2() {
        // Arrange
        val cardId = 202
        
        every { mockPlayer.bloomCount } returns 1
        
        // Bloom 1 is empty
        every { mockGameCardIds1.isEmpty() } returns true
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_1) } returns mockGameCardIds1
        
        // Bloom 2 has cards
        every { mockGameCardIds2.isEmpty() } returns false
        every { mockGameCardIds2.cardIds } returns listOf(cardId)
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_2) } returns mockGameCardIds2
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify { mockGrove.removeCard(cardId) }
        verify { mockPlayer.addCardToCompost(cardId) }
        
        // Verify we didn't check stack 3
        verify(exactly = 0) { mockGrove.getCardsFor(MarketStackID.BLOOM_3) }
    }
    
    @Test
    fun giftTo_whenPlayerIsNotReadyAndOnlyBloom3StackHasCards_givesCardFromBloom3() {
        // Arrange
        val cardId = 303
        
        every { mockPlayer.bloomCount } returns 1
        
        // Bloom 1 is empty
        every { mockGameCardIds1.isEmpty() } returns true
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_1) } returns mockGameCardIds1
        
        // Bloom 2 is empty
        every { mockGameCardIds2.isEmpty() } returns true
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_2) } returns mockGameCardIds2
        
        // Bloom 3 has cards
        every { mockGameCardIds3.isEmpty() } returns false
        every { mockGameCardIds3.cardIds } returns listOf(cardId)
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_3) } returns mockGameCardIds3
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify { mockGrove.removeCard(cardId) }
        verify { mockPlayer.addCardToCompost(cardId) }
    }
    
    @Test
    fun giftTo_whenPlayerIsNotReadyButNoBloomStacksHaveCards_doesNothing() {
        // Arrange
        every { mockPlayer.bloomCount } returns 1
        
        // All Bloom stacks are empty
        every { mockGameCardIds1.isEmpty() } returns true
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_1) } returns mockGameCardIds1
        
        every { mockGameCardIds2.isEmpty() } returns true
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_2) } returns mockGameCardIds2
        
        every { mockGameCardIds3.isEmpty() } returns true
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_3) } returns mockGameCardIds3
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify(exactly = 0) { mockGrove.removeCard(any<CardID>()) }
        verify(exactly = 0) { mockPlayer.addCardToCompost(any<CardID>()) }
    }
    
    @Test
    fun giftTo_whenPlayerIsReady_doesNothing() {
        // Arrange
        every { mockPlayer.bloomCount } returns 2
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify(exactly = 0) { mockGrove.getCardsFor(any()) }
        verify(exactly = 0) { mockGrove.removeCard(any<CardID>()) }
        verify(exactly = 0) { mockPlayer.addCardToCompost(any<CardID>()) }
    }
    
    @Test
    fun giftTo_checksReadinessBeforeAccessingMarket() {
        // Arrange
        every { mockPlayer.bloomCount } returns 2
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verifyOrder {
            mockPlayer.bloomCount
            // The following should not happen since player is ready
            // mockMarket.getCardsFor(any())
        }
    }
    
    @Test
    fun giftTo_whenPlayerNeedsOneBloomCard_givesOneCard() {
        // Arrange
        val cardId = 101
        every { mockPlayer.bloomCount } returns 1
        every { mockGameCardIds1.isEmpty() } returns false
        every { mockGameCardIds1.cardIds } returns listOf(cardId)
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_1) } returns mockGameCardIds1
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify { mockGrove.removeCard(cardId) }
        verify { mockPlayer.addCardToCompost(cardId) }
    }
    
    @Test
    fun giftTo_whenPlayerCanGetABloomCard_getsACard() {
        // Arrange
        val cardId1 = 101
        val cardId2 = 102
        every { mockPlayer.bloomCount } returns 0
        every { mockGameCardIds1.isEmpty() } returns false
        every { mockGameCardIds1.cardIds } returns listOf(cardId1, cardId2)
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_1) } returns mockGameCardIds1
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify { mockGrove.removeCard(cardId1) }
        verify { mockPlayer.addCardToCompost(cardId1) }
    }
    
    @Test
    fun giftTo_whenPlayerAlreadyHasEnoughBloomCards_doesNothing() {
        // Arrange
        every { mockPlayer.bloomCount } returns 2
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify(exactly = 0) { mockGrove.getCardsFor(any()) }
        verify(exactly = 0) { mockGrove.removeCard(any<CardID>()) }
        verify(exactly = 0) { mockPlayer.addCardToCompost(any<CardID>()) }
    }
    
    @Test
    fun giftTo_whenNoBloomCardsAvailable_doesNothing() {
        // Arrange
        every { mockPlayer.bloomCount } returns 0
        
        // All Bloom stacks are empty
        every { mockGameCardIds1.isEmpty() } returns true
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_1) } returns mockGameCardIds1
        
        every { mockGameCardIds2.isEmpty() } returns true
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_2) } returns mockGameCardIds2
        
        every { mockGameCardIds3.isEmpty() } returns true
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_3) } returns mockGameCardIds3
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify(exactly = 0) { mockGrove.removeCard(any<CardID>()) }
        verify(exactly = 0) { mockPlayer.addCardToCompost(any<CardID>()) }
    }
    
    @Test
    fun giftTo_whenMarketStackReturnsNull_skipsToNextStack() {
        // Arrange
        val cardId = 202
        
        every { mockPlayer.bloomCount } returns 1
        
        // Bloom 1 returns null
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_1) } returns null
        
        // Bloom 2 has cards
        every { mockGameCardIds2.isEmpty() } returns false
        every { mockGameCardIds2.cardIds } returns listOf(cardId)
        every { mockGrove.getCardsFor(MarketStackID.BLOOM_2) } returns mockGameCardIds2
        
        // Act
        SUT.giftTo(mockPlayer)
        
        // Assert
        verify { mockGrove.removeCard(cardId) }
        verify { mockPlayer.addCardToCompost(cardId) }
    }
} 
