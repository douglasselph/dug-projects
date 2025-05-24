package dugsolutions.leaf.game.battle

import dugsolutions.leaf.player.PlayerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse

class ProcessBattlePhaseTest {

    private lateinit var mockHandleBattleEffects: HandleBattleEffects
    private lateinit var mockHandleDeliverDamage: HandleDeliverDamage
    private lateinit var mockHandleAbsorbDamage: HandleAbsorbDamage
    
    private lateinit var player1: PlayerTD
    private lateinit var player2: PlayerTD
    private lateinit var player3: PlayerTD
    
    private lateinit var SUT: ProcessBattlePhase

    @BeforeEach
    fun setup() {
        mockHandleBattleEffects = mockk(relaxed = true)
        mockHandleDeliverDamage = mockk(relaxed = true)
        mockHandleAbsorbDamage = mockk(relaxed = true)
        
        player1 = PlayerTD("Player 1", 1)
        player2 = PlayerTD("Player 2", 2)
        player3 = PlayerTD("Player 3", 3)
        
        SUT = ProcessBattlePhase(
            mockHandleBattleEffects,
            mockHandleDeliverDamage,
            mockHandleAbsorbDamage
        )
    }

    @Test
    fun invoke_callsHandlersInCorrectOrder() {
        // Arrange
        val players = listOf(player1, player2, player3)
        
        // Act
        SUT(players)
        
        // Assert - verify the overall order of operations
        verifyOrder {
            // First, handle battle effects for each player
            mockHandleBattleEffects(player1)
            mockHandleBattleEffects(player2)
            mockHandleBattleEffects(player3)
            
            // Then deliver damage once for all players
            mockHandleDeliverDamage(players)
            
            // Then handle damage absorption for each player
            mockHandleAbsorbDamage(player1)
            mockHandleAbsorbDamage(player2)
            mockHandleAbsorbDamage(player3)
        }
    }
    
    @Test
    fun invoke_resetsDormantFlagForAllPlayers() {
        // Arrange
        player1.isDormant = true
        player2.isDormant = true
        player3.isDormant = false
        
        val players = listOf(player1, player2, player3)
        
        // Act
        SUT(players)
        
        // Assert
        assertFalse(player1.isDormant, "Player 1 should not be dormant after battle phase")
        assertFalse(player2.isDormant, "Player 2 should not be dormant after battle phase")
        assertFalse(player3.isDormant, "Player 3 should not be dormant after battle phase")
    }
    
    @Test
    fun invoke_resetsDormantFlagForSinglePlayer() {
        // Arrange
        player1.isDormant = true
        val singlePlayer = listOf(player1)
        
        // Act
        SUT(singlePlayer)
        
        // Assert
        assertFalse(player1.isDormant, "Single player should not be dormant after battle phase")
    }
    
    @Test
    fun invoke_resetsDormantFlagAfterAllOtherOperations() {
        // Arrange
        player1.isDormant = true
        val players = listOf(player1)
        
        // Act
        SUT(players)
        
        // Assert - verify that dormant flag is reset after all other operations
        verifySequence {
            mockHandleBattleEffects(player1)
            mockHandleDeliverDamage(players)
            mockHandleAbsorbDamage(player1)
        }
        assertFalse(player1.isDormant, "Dormant flag should be reset after all other operations")
    }
    
    @Test
    fun invoke_withEmptyPlayerList_doesNotCallHandlers() {
        // Arrange
        val emptyPlayerList = emptyList<PlayerTD>()
        
        // Act
        SUT(emptyPlayerList)
        
        // Assert
        verify(exactly = 0) { mockHandleBattleEffects(any()) }
        verify(exactly = 1) { mockHandleDeliverDamage(emptyPlayerList) }
        verify(exactly = 0) { mockHandleAbsorbDamage(any()) }
    }
    
    @Test
    fun invoke_procedureStepsExecutedInCorrectSequence() {
        // Arrange
        val players = listOf(player1, player2)
        
        // Act
        SUT(players)
        
        // Assert - verify exact sequence of all interactions with mocks
        verifySequence {
            // First step: handle battle effects for each player
            mockHandleBattleEffects(player1)
            mockHandleBattleEffects(player2)
            
            // Second step: deliver damage once with all players
            mockHandleDeliverDamage(players)
            
            // Third step: handle damage absorption for each player
            mockHandleAbsorbDamage(player1)
            mockHandleAbsorbDamage(player2)
            
            // No more calls expected
        }
    }
} 
