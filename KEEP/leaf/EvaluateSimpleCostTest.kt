package dugsolutions.leaf.game.purchase.cost

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.game.turn.cost.EvaluateSimpleCost
import dugsolutions.leaf.player.Player
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EvaluateSimpleCostTest {
    
    private lateinit var evaluateSimpleCost: EvaluateSimpleCost
    private lateinit var mockPlayer: Player
    private lateinit var mockDie1: Die
    private lateinit var mockDie2: Die
    
    @BeforeEach
    fun setup() {
        evaluateSimpleCost = EvaluateSimpleCost()
        mockPlayer = mockk(relaxed = true)
        mockDie1 = mockk()
        mockDie2 = mockk()
    }
    
    @Test
    fun invoke_withEmptyCombination_doesNothing() {
        // Arrange
        val combination = emptyList<Die>()
        
        // Act
        evaluateSimpleCost(mockPlayer, combination)
        
        // Assert - No interactions with player for discarding or removing pips
        verify(exactly = 0) { mockPlayer.discard(any<Die>()) }
        verify(exactly = 0) { mockPlayer.pipModifierRemove(any()) }
    }
    
    @Test
    fun invoke_withOnlyDice_discardsAllDice() {
        // Arrange
        val dice = listOf(mockDie1, mockDie2)

        // Act
        evaluateSimpleCost(mockPlayer, dice)
        
        // Assert
        verify(exactly = 1) { mockPlayer.discard(mockDie1) }
        verify(exactly = 1) { mockPlayer.discard(mockDie2) }
        verify(exactly = 0) { mockPlayer.pipModifierRemove(any()) }
    }
    
    @Test
    fun invoke_withMixedDiceAndPips_discardsAndRemovesAll() {
        // Arrange
        val dice = listOf(mockDie1, mockDie2)
        val combination = dice
        
        // Act
        evaluateSimpleCost(mockPlayer, combination)
        
        // Assert
        verify(exactly = 1) { mockPlayer.discard(mockDie1) }
        verify(exactly = 1) { mockPlayer.discard(mockDie2) }
        verify(exactly = 1) { mockPlayer.pipModifierRemove(2) }
        verify(exactly = 1) { mockPlayer.pipModifierRemove(4) }
    }
    
    @Test
    fun invoke_appliesCostInOrder() {
        // Arrange
        val dice = listOf(mockDie1, mockDie2)
        val combination = dice
        
        // Act
        evaluateSimpleCost(mockPlayer, combination)
        
        // Assert - Verify the order of operations (dice first, then pips)
        verifyOrder {
            mockPlayer.discard(mockDie1)
            mockPlayer.discard(mockDie2)
            mockPlayer.pipModifierRemove(6)
        }
    }
} 
