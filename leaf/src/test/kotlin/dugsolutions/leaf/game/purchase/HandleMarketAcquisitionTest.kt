package dugsolutions.leaf.game.purchase

import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.turn.select.SelectPossibleCards
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleMarketAcquisitionTest {

    private lateinit var mockSelectPossibleCards: SelectPossibleCards
    private lateinit var mockPurchaseItem: PurchaseItem
    private lateinit var mockManagePurchasedFloralTypes: ManagePurchasedFloralTypes
    private lateinit var mockPlayer: Player
    private lateinit var sampleDie: SampleDie

    private lateinit var SUT: HandleMarketAcquisition

    @BeforeEach
    fun setup() {
        mockSelectPossibleCards = mockk(relaxed = true)
        mockPurchaseItem = mockk(relaxed = true)
        mockManagePurchasedFloralTypes = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        sampleDie = SampleDie()
        
        SUT = HandleMarketAcquisition(
            mockSelectPossibleCards,
            mockPurchaseItem,
            mockManagePurchasedFloralTypes
        )
    }

    @Test
    fun invoke_whenPlayerIsDormant_doesNothing() {
        // Arrange
        every { mockPlayer.isDormant } returns true
        
        // Act
        SUT(mockPlayer)
        
        // Assert
        verify(exactly = 0) { mockManagePurchasedFloralTypes.clear() }
        verify(exactly = 0) { mockSelectPossibleCards(any()) }
        verify(exactly = 0) { mockPurchaseItem(any(), any()) }
    }

    @Test
    fun invoke_whenNoDiceInHand_onlyClearsTypes() {
        // Arrange
        every { mockPlayer.isDormant } returns false
        every { mockPlayer.diceInHand.isNotEmpty() } returns false
        
        // Act
        SUT(mockPlayer)
        
        // Assert
        verify { mockManagePurchasedFloralTypes.clear() }
        verify(exactly = 0) { mockSelectPossibleCards(any()) }
        verify(exactly = 0) { mockPurchaseItem(any(), any()) }
    }

    @Test
    fun invoke_withDiceInHand_loopsUntilNoDice() {
        // Arrange
        every { mockPlayer.isDormant } returns false
        
        // Set up to run loop 3 times
        every { mockPlayer.diceInHand.isNotEmpty() } returnsMany listOf(true, true, true, false)
        
        val possibleCards1 = listOf(mockk<dugsolutions.leaf.components.GameCard>())
        val possibleCards2 = listOf(mockk<dugsolutions.leaf.components.GameCard>())
        val possibleCards3 = listOf(mockk<dugsolutions.leaf.components.GameCard>())
        
        every { mockSelectPossibleCards(mockPlayer) } returnsMany listOf(possibleCards1, possibleCards2, possibleCards3)
        
        // Act
        SUT(mockPlayer)
        
        // Assert
        verify { mockManagePurchasedFloralTypes.clear() }
        verify(exactly = 3) { mockSelectPossibleCards(mockPlayer) }
        verify { mockPurchaseItem(mockPlayer, possibleCards1) }
        verify { mockPurchaseItem(mockPlayer, possibleCards2) }
        verify { mockPurchaseItem(mockPlayer, possibleCards3) }
    }

    @Test
    fun invoke_withEmptyPossibleCards_stillContinuesLoop() {
        // Arrange
        every { mockPlayer.isDormant } returns false
        
        // Set up to run loop 2 times
        every { mockPlayer.diceInHand.isNotEmpty() } returnsMany listOf(true, true, false)
        
        val emptyCards = emptyList<dugsolutions.leaf.components.GameCard>()
        val possibleCards = listOf(mockk<dugsolutions.leaf.components.GameCard>())
        
        every { mockSelectPossibleCards(mockPlayer) } returnsMany listOf(emptyCards, possibleCards)
        
        // Act
        SUT(mockPlayer)
        
        // Assert
        verify { mockManagePurchasedFloralTypes.clear() }
        verify(exactly = 2) { mockSelectPossibleCards(mockPlayer) }
        verify { mockPurchaseItem(mockPlayer, emptyCards) }
        verify { mockPurchaseItem(mockPlayer, possibleCards) }
    }
} 
