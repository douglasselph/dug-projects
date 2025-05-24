package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for PlayerSetupForBattlePhase
 */
class PlayerSetupForBattlePhaseTest {
    
    private lateinit var mockGrove: Grove
    private lateinit var mockPlayer: Player
    private lateinit var mockPlayerBattlePhaseCheck: PlayerBattlePhaseCheck
    private lateinit var mockBonusDie: Die
    private lateinit var mockMarketBonusDie: Die

    private lateinit var SUT: PlayerSetupForBattlePhase

    @BeforeEach
    fun setup() {
        mockGrove = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockPlayerBattlePhaseCheck = mockk(relaxed = true)
        mockBonusDie = mockk(relaxed = true)
        mockMarketBonusDie = mockk(relaxed = true)
        
        SUT = PlayerSetupForBattlePhase(mockGrove)
    }
    
    @Test
    fun invoke_whenPlayerHasBonusDie_addsDieToCompostAndClearsBonusDie() {
        // Arrange
        every { mockPlayer.bonusDie } returns mockBonusDie
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verifyOrder {
            mockPlayer.bonusDie
            mockPlayer.addDieToCompost(mockBonusDie)
            mockPlayer.bonusDie = null
            mockPlayer.reset()
        }
        
        // Verify we didn't access unused paths
        verify(exactly = 0) { mockGrove.useNextBonusDie }
        verify(exactly = 0) { mockPlayerBattlePhaseCheck.giftTo(any()) }
    }
    
    @Test
    fun invoke_whenPlayerHasNoBonusDieAndMarketHasBonusDie_addsDieFromMarketToCompost() {
        // Arrange
        every { mockPlayer.bonusDie } returns null
        every { mockGrove.useNextBonusDie } returns mockMarketBonusDie
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verifyOrder {
            mockPlayer.bonusDie
            mockGrove.useNextBonusDie
            mockPlayer.addDieToCompost(mockMarketBonusDie)
            mockPlayerBattlePhaseCheck.giftTo(mockPlayer)
            mockPlayer.reset()
        }
    }
    
    @Test
    fun invoke_whenPlayerHasNoBonusDieAndMarketHasNoBonusDie_onlyGiftsToPlayer() {
        // Arrange
        every { mockPlayer.bonusDie } returns null
        every { mockGrove.useNextBonusDie } returns null
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verifyOrder {
            mockPlayer.bonusDie
            mockGrove.useNextBonusDie
            mockPlayerBattlePhaseCheck.giftTo(mockPlayer)
            mockPlayer.reset()
        }
        
        // Verify we didn't add any die to compost 
        verify(exactly = 0) { mockPlayer.addDieToCompost(any()) }
    }
    
    @Test
    fun invoke_alwaysResetsPlayerAndClearsDormantFlag() {
        // Arrange
        every { mockPlayer.bonusDie } returns mockBonusDie
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verify { mockPlayer.reset() }
    }
    
    @Test
    fun invoke_whenPlayerHasNoBonusDie_callsGiftTo() {
        // Arrange
        every { mockPlayer.bonusDie } returns null
        every { mockGrove.useNextBonusDie } returns mockMarketBonusDie
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verify { mockPlayerBattlePhaseCheck.giftTo(mockPlayer) }
    }
    
    @Test
    fun invoke_callsTrashSeedlingCards() {
        // Arrange
        every { mockPlayer.bonusDie } returns mockBonusDie
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verify { mockPlayer.trashSeedlingCards() }
    }
    
    @Test
    fun invoke_callsDrawHand() {
        // Arrange
        every { mockPlayer.bonusDie } returns mockBonusDie
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verify { mockPlayer.drawHand() }
    }
    
    @Test
    fun invoke_whenPlayerHasBonusDie_doesNotSetDormantFlag() {
        // Arrange
        every { mockPlayer.bonusDie } returns mockBonusDie
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verify(exactly = 0) { mockPlayer.isDormant = true }
    }
    
    @Test
    fun invoke_whenPlayerHasNoBonusDieAndMarketHasBonusDie_setsDormantFlag() {
        // Arrange
        every { mockPlayer.bonusDie } returns null
        every { mockGrove.useNextBonusDie } returns mockMarketBonusDie
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verify { mockPlayer.isDormant = true }
    }
    
    @Test
    fun invoke_whenPlayerHasNoBonusDieAndMarketHasNoBonusDie_setsDormantFlag() {
        // Arrange
        every { mockPlayer.bonusDie } returns null
        every { mockGrove.useNextBonusDie } returns null
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verify { mockPlayer.isDormant = true }
    }
    
    @Test
    fun invoke_setsDormantFlagBeforeReset() {
        // Arrange
        every { mockPlayer.bonusDie } returns null
        every { mockGrove.useNextBonusDie } returns mockMarketBonusDie
        
        // Act
        SUT(mockPlayer, mockPlayerBattlePhaseCheck)
        
        // Assert
        verifyOrder {
            mockPlayer.bonusDie
            mockGrove.useNextBonusDie
            mockPlayer.addDieToCompost(mockMarketBonusDie)
            mockPlayerBattlePhaseCheck.giftTo(mockPlayer)
            mockPlayer.isDormant = true
            mockPlayer.reset()
        }
    }
} 
