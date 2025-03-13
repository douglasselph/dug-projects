package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.market.Market
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PlayerReadyForBattlePhaseTest {

    companion object {
        private const val PLAYER_ID = 1
        private const val PLAYER_NAME = "Test Player"
    }

    private lateinit var mockMarket: Market
    private lateinit var mockPlayer: Player
    private lateinit var mockBonusDie: Die

    private lateinit var SUT: PlayerReadyForBattlePhase

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        mockMarket = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockBonusDie = mockk(relaxed = true)
        
        // Setup basic player properties
        every { mockPlayer.id } returns PLAYER_ID
        every { mockPlayer.name } returns PLAYER_NAME
        every { mockPlayer.isDormant } returns false
        every { mockPlayer.bonusDie } returns null
        
        // Setup market properties
        every { mockMarket.useNextBonusDie } returns mockBonusDie
        
        // Create the PrepareBattlePhase instance
        SUT = PlayerReadyForBattlePhase(mockMarket)
    }

    @Test
    fun invoke_whenPlayerIsDormant_awakensPlayerAndReturnsTrue() {
        // Arrange
        every { mockPlayer.isDormant } returns true
        
        // Act
        val result = SUT(mockPlayer, false)
        
        // Assert
        assertTrue(result)
        verify { mockPlayer.isDormant = false }
    }

    @Test
    fun invoke_whenPlayerHasBonusDie_returnsTrue() {
        // Arrange
        every { mockPlayer.bonusDie } returns mockBonusDie
        
        // Act
        val result = SUT(mockPlayer, false)
        
        // Assert
        assertTrue(result)
        verify(exactly = 0) { mockPlayer.reset() }
        verify(exactly = 0) { mockPlayer.isDormant = true }
    }

    @Test
    fun invoke_whenReadyBattlePhaseAndNoBonusDie_preparesPlayerAndReturnsTrue() {
        // Arrange
        every { mockPlayer.bonusDie } returns null
        
        // Act
        val result = SUT(mockPlayer, true)
        
        // Assert
        assertTrue(result)
        verify { mockPlayer.bonusDie = mockBonusDie }
        verify { mockPlayer.reset() }
        verify { mockPlayer.isDormant = true }
    }

    @Test
    fun invoke_whenNotReadyBattlePhaseAndNoBonusDie_returnsFalse() {
        // Arrange
        every { mockPlayer.bonusDie } returns null
        
        // Act
        val result = SUT(mockPlayer, false)
        
        // Assert
        assertFalse(result)
        verify(exactly = 0) { mockPlayer.bonusDie = any() }
        verify(exactly = 0) { mockPlayer.reset() }
        verify(exactly = 0) { mockPlayer.isDormant = true }
    }

    @Test
    fun invoke_whenPlayerIsDormantAndHasBonusDie_awakensPlayerAndReturnsTrue() {
        // Arrange
        every { mockPlayer.isDormant } returns true
        every { mockPlayer.bonusDie } returns mockBonusDie
        
        // Act
        val result = SUT(mockPlayer, false)
        
        // Assert
        assertTrue(result)
        verify { mockPlayer.isDormant = false }
        verify(exactly = 0) { mockPlayer.bonusDie = any() }
        verify(exactly = 0) { mockPlayer.reset() }
    }

    @Test
    fun invoke_whenPlayerIsDormantAndReadyBattlePhase_awakensPlayerAndReturnsTrue() {
        // Arrange
        every { mockPlayer.isDormant } returns true
        
        // Act
        val result = SUT(mockPlayer, true)
        
        // Assert
        assertTrue(result)
        verify { mockPlayer.isDormant = false }
        verify(exactly = 0) { mockPlayer.bonusDie = any() }
        verify(exactly = 0) { mockPlayer.reset() }
    }
} 
