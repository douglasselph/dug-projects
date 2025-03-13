package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.RandomizerTD
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleGetTargetTest {

    // Test subject
    private lateinit var SUT: HandleGetTarget
    
    // Dependencies
    private lateinit var randomizerTD: RandomizerTD
    
    // Test data
    private lateinit var currentPlayer: Player
    private lateinit var validTarget1: Player
    private lateinit var validTarget2: Player
    private lateinit var hitTarget: Player
    private lateinit var allPlayers: List<Player>
    
    @BeforeEach
    fun setup() {
        // Arrange
        randomizerTD = RandomizerTD()
        
        // Create test players
        currentPlayer = mockk(relaxed = true) {
            every { id } returns 1
        }
        
        validTarget1 = mockk(relaxed = true) {
            every { id } returns 2
            every { wasHit } returns false
        }
        
        validTarget2 = mockk(relaxed = true) {
            every { id } returns 3
            every { wasHit } returns false
        }
        
        hitTarget = mockk(relaxed = true) {
            every { id } returns 4
            every { wasHit } returns true
        }
        
        allPlayers = listOf(currentPlayer, validTarget1, validTarget2, hitTarget)
        
        // Create the test subject
        SUT = HandleGetTarget(randomizerTD)
    }

    @Test
    fun invoke_withMultipleValidTargets_returnsRandomTarget() {
        // Arrange - setting up randomizer to return 0 (select first valid target)
        randomizerTD.setValues(listOf(0))
        
        // Act
        val result = SUT(currentPlayer, allPlayers)
        
        // Assert
        assertEquals(validTarget1, result, "Should return the first valid target when random value is 0")
        
        // Check with a different random selection - setting up to return 1 (select second valid target)
        randomizerTD.setValues(listOf(1))
        val result2 = SUT(currentPlayer, allPlayers)
        assertEquals(validTarget2, result2, "Should return the second valid target when random value is 1")
    }
    
    @Test
    fun invoke_withNoValidTargets_returnsNull() {
        // Arrange
        val allTargetsHit = listOf(
            currentPlayer,
            mockk(relaxed = true) { 
                every { wasHit } returns true 
            },
            mockk(relaxed = true) { 
                every { wasHit } returns true 
            }
        )
        
        // Act
        val result = SUT(currentPlayer, allTargetsHit)
        
        // Assert
        assertNull(result, "Should return null when no valid targets are available")
    }
    
    @Test
    fun invoke_withOnlyCurrentPlayer_returnsNull() {
        // Arrange
        val onlyCurrentPlayer = listOf(currentPlayer)
        
        // Act
        val result = SUT(currentPlayer, onlyCurrentPlayer)
        
        // Assert
        assertNull(result, "Should return null when only the current player is in the list")
    }
    
    @Test
    fun invoke_withOneValidTarget_returnsThatTarget() {
        // Arrange
        val oneValidTarget = listOf(currentPlayer, validTarget1, hitTarget)
        randomizerTD.setValues(listOf(0)) // Any value works since there's only one valid target
        
        // Act
        val result = SUT(currentPlayer, oneValidTarget)
        
        // Assert
        assertEquals(validTarget1, result, "Should return the only valid target")
    }
    
    @Test
    fun invoke_withDifferentRandomValues_selectsCorrectTarget() {
        // Arrange
        // First test with value that will select the first target
        randomizerTD.setValues(listOf(0))
        
        // Act
        val result1 = SUT(currentPlayer, allPlayers)
        
        // Assert
        assertEquals(validTarget1, result1, "Should select first valid target with random value 0")
        
        // Now test with value that will select the second target
        randomizerTD.setValues(listOf(1))
        
        // Act
        val result2 = SUT(currentPlayer, allPlayers)
        
        // Assert
        assertEquals(validTarget2, result2, "Should select second valid target with random value 1")
    }
    
    @Test
    fun invoke_whenPlayersContainsDuplicates_filtersCorrectly() {
        // Arrange
        val playersWithDuplicates = listOf(currentPlayer, validTarget1, validTarget1, hitTarget)
        randomizerTD.setValues(listOf(0))
        
        // Act
        val result = SUT(currentPlayer, playersWithDuplicates)
        
        // Assert
        assertEquals(validTarget1, result, "Should handle duplicates in the player list")
    }
    
    @Test
    fun invoke_whenEmptyPlayerList_returnsNull() {
        // Arrange
        val emptyList = emptyList<Player>()
        
        // Act
        val result = SUT(currentPlayer, emptyList)
        
        // Assert
        assertNull(result, "Should return null when player list is empty")
    }
} 
