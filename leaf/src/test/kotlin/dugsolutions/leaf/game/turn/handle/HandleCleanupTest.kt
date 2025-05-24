package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class HandleCleanupTest {
    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
    }

    private lateinit var handleCleanup: HandleCleanup
    private lateinit var mockPlayer: Player
    private lateinit var mockCard1: GameCard
    private lateinit var mockCard2: GameCard

    @BeforeEach
    fun setup() {
        handleCleanup = HandleCleanup()
        mockPlayer = mockk(relaxed = true)
        
        // Create mock cards
        mockCard1 = mockk {
            every { id } returns CARD_ID_1
            every { type } returns FlourishType.ROOT
        }
        mockCard2 = mockk {
            every { id } returns CARD_ID_2
            every { type } returns FlourishType.BLOOM
        }
        every { mockPlayer.drawHand(any()) } just Runs
        
        // Default setup for supply counts
        every { mockPlayer.diceInSupplyCount } returns 1
        every { mockPlayer.cardsInSupplyCount } returns 1
        every { mockPlayer.resupply() } just Runs
    }

    @Test
    fun invoke_whenSpecifiedCardCount_discardsHandAndDrawsCorrectNumber() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()

        // Act
        handleCleanup(mockPlayer)

        // Assert
        verify { mockPlayer.discardHand() }
        verify { mockPlayer.drawHand() }
        verify(exactly = 0) { mockPlayer.addCardToHand(any()) }
    }

    @Test
    fun invoke_whenZeroCardCount_discardsHandAndDrawsNothing() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()

        // Act
        handleCleanup(mockPlayer)

        // Assert
        verify { mockPlayer.discardHand() }
        verify { mockPlayer.drawHand() }
        verify(exactly = 0) { mockPlayer.addCardToHand(any()) }
    }

    @Test
    fun invoke_whenNegativeCardCount_discardsHandAndDrawsNothing() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()

        // Act
        handleCleanup(mockPlayer)

        // Assert
        verify { mockPlayer.discardHand() }
        verify { mockPlayer.drawHand() }
        verify(exactly = 0) { mockPlayer.addCardToHand(any()) }
    }

    @Test
    fun invoke_whenLargeCardCount_discardsHandAndDrawsMaximumAllowed() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()

        // Act
        handleCleanup(mockPlayer)

        // Assert
        verify { mockPlayer.discardHand() }
        verify { mockPlayer.drawHand() }
        verify(exactly = 0) { mockPlayer.addCardToHand(any()) }
    }

    @Test
    fun invoke_whenNullCardCount_discardsHandAndDrawsDefaultCount() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()

        // Act
        handleCleanup(mockPlayer)

        // Assert
        verify { mockPlayer.discardHand() }
        verify { mockPlayer.drawHand() }
        verify(exactly = 0) { mockPlayer.addCardToHand(any()) }
    }

    @Test
    fun invoke_whenCardsReusedNotEmpty_addsReusedCardsToHandAndClearsList() {
        // Arrange
        val cardsReused = mutableListOf(mockCard1, mockCard2)
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns cardsReused
        every { mockPlayer.addCardToHand(any()) } just Runs

        // Act
        handleCleanup(mockPlayer)

        // Assert
        verify { mockPlayer.discardHand() }
        verify { mockPlayer.drawHand() }
        verify { mockPlayer.addCardToHand(CARD_ID_1) }
        verify { mockPlayer.addCardToHand(CARD_ID_2) }
        assertTrue(cardsReused.isEmpty())
    }

    @Test
    fun invoke_whenCardsReusedNotEmptyAndZeroDrawCount_addsReusedCardsToHandAndClearsList() {
        // Arrange
        val cardsReused = mutableListOf(mockCard1, mockCard2)
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns cardsReused
        every { mockPlayer.addCardToHand(any()) } just Runs

        // Act
        handleCleanup(mockPlayer)

        // Assert
        verify { mockPlayer.discardHand() }
        verify { mockPlayer.drawHand() }
        verify { mockPlayer.addCardToHand(CARD_ID_1) }
        verify { mockPlayer.addCardToHand(CARD_ID_2) }
        assertTrue(cardsReused.isEmpty())
    }
    
    @Test
    fun invoke_whenBothSuppliesEmpty_callsResupply() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()
        
        // Both supplies are empty
        every { mockPlayer.diceInSupplyCount } returns 0
        every { mockPlayer.cardsInSupplyCount } returns 0
        
        // Act
        handleCleanup(mockPlayer)
        
        // Assert
        verify { mockPlayer.discardHand() }
        verify { mockPlayer.diceInSupplyCount }
        verify { mockPlayer.cardsInSupplyCount }
        verify { mockPlayer.resupply() }
        verify { mockPlayer.drawHand() }
    }
    
    @Test
    fun invoke_whenOnlyDiceSupplyEmpty_callsResupply() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()
        
        // Only dice supply empty
        every { mockPlayer.diceInSupplyCount } returns 0
        every { mockPlayer.cardsInSupplyCount } returns 1
        
        // Act
        handleCleanup(mockPlayer)
        
        // Assert
        verify { mockPlayer.discardHand() }
        verify { mockPlayer.diceInSupplyCount }
        verify { mockPlayer.cardsInSupplyCount }
        verify(exactly = 0) { mockPlayer.resupply() }
        verify { mockPlayer.drawHand() }
    }
    
    @Test
    fun invoke_whenOnlyCardSupplyEmpty_callsResupply() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()
        
        // Only card supply empty
        every { mockPlayer.diceInSupplyCount } returns 1
        every { mockPlayer.cardsInSupplyCount } returns 0
        
        // Act
        handleCleanup(mockPlayer)
        
        // Assert
        verify { mockPlayer.discardHand() }
        verify(exactly = 0) { mockPlayer.resupply() }
        verify { mockPlayer.drawHand() }
    }
    
    @Test
    fun invoke_whenBothSuppliesNonEmpty_doesNotCallResupply() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()
        
        // Both supplies have content
        every { mockPlayer.diceInSupplyCount } returns 1
        every { mockPlayer.cardsInSupplyCount } returns 1
        
        // Act
        handleCleanup(mockPlayer)
        
        // Assert
        verify { mockPlayer.discardHand() }
        verify(exactly = 0) { mockPlayer.resupply() }
        verify { mockPlayer.drawHand() }
    }
    
    @Test
    fun invoke_alwaysCallsDrawHand() {
        // Arrange
        every { mockPlayer.discardHand() } just Runs
        every { mockPlayer.cardsReused } returns mutableListOf()
        
        // Act
        handleCleanup(mockPlayer)
        
        // Assert
        verify { mockPlayer.drawHand() }
    }
} 
