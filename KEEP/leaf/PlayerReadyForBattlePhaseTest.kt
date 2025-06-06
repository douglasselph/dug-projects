package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.grove.Grove
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

    private lateinit var mockGrove: Grove
    private lateinit var mockPlayer: Player
    private lateinit var mockBonusDie: Die

    private lateinit var SUT: PlayerReadyForBattlePhase

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        mockGrove = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockBonusDie = mockk(relaxed = true)
        
        // Setup basic player properties
        every { mockPlayer.id } returns PLAYER_ID
        every { mockPlayer.name } returns PLAYER_NAME
        every { mockPlayer.bonusDie } returns null
        
        // Setup market properties
        every { mockGrove.useNextBonusDie } returns mockBonusDie
        
        // Create the PrepareBattlePhase instance
        SUT = PlayerReadyForBattlePhase(mockGrove)
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
    }

    @Test
    fun invoke_whenPlayerHasBonusDie_awakensPlayerAndReturnsTrue() {
        // Arrange
        every { mockPlayer.bonusDie } returns mockBonusDie
        
        // Act
        val result = SUT(mockPlayer, false)
        
        // Assert
        assertTrue(result)
        verify(exactly = 0) { mockPlayer.bonusDie = any() }
        verify(exactly = 0) { mockPlayer.reset() }
    }

    @Test
    fun invoke_whenReadyBattlePhase_awakensPlayerAndReturnsTrue() {
        // Arrange

        // Act
        val result = SUT(mockPlayer, true)
        
        // Assert
        assertTrue(result)
        verify(exactly = 0) { mockPlayer.bonusDie = any() }
        verify(exactly = 0) { mockPlayer.reset() }
    }
} 
