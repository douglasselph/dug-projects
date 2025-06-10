package dugsolutions.leaf.game.acquire.cost

import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.DieValues
import dugsolutions.leaf.game.acquire.domain.Adjusted
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.domain.AppliedEffect
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplyEffectsTest {

    private lateinit var player: PlayerTD
    private lateinit var testDie1: DieValue
    private lateinit var testDie2: DieValue
    
    private lateinit var SUT: ApplyEffects

    @BeforeEach
    fun setup() {
        player = PlayerTD("Test Player", 1)
        
        // Create test dice
        testDie1 = DieValue(6, 3) // 6-sided die with value 3
        testDie2 = DieValue(8, 5) // 8-sided die with value 5
        
        // Add dice to player's hand
        player.diceInHand.clear()
        player.addDieToHand(testDie1)
        player.addDieToHand(testDie2)
        
        // Clear any existing effects
        player.delayedEffectList.clear()
        
        SUT = ApplyEffects()
    }

    @Test
    fun invoke_withAdjustByAmount_adjustsDieAndRemovesEffect() {
        // Arrange
        val adjustment = 2
        val adjustEffect = AppliedEffect.AdjustDieRoll(adjustment)
        player.delayedEffectList.addAll(listOf(adjustEffect))
        
        val adjusted = Adjusted.ByAmount(testDie1, adjustment)
        val dieValues = DieValues(listOf(testDie1, testDie2))
        val combination = Combination(
            values = dieValues,
            addToTotal = 0,
            adjusted = listOf(adjusted)
        )
        
        // Initial value
        assertEquals(3, testDie1.value)
        
        // Act
        SUT(player, combination)
        
        // Assert
        assertEquals(5, player.diceInHand[0]?.value) // 3 + 2 = 5
        assertTrue(player.delayedEffectList.isEmpty()) // Effect should be removed
    }
    
    @Test
    fun invoke_withAdjustToMax_adjustsDieToMaximumAndRemovesEffect() {
        // Arrange
        val adjustEffect = AppliedEffect.AdjustDieToMax()
        player.delayedEffectList.addAll(listOf(adjustEffect))
        
        val adjusted = Adjusted.ToMax(testDie1)
        val dieValues = DieValues(listOf(testDie1, testDie2))
        val combination = Combination(
            values = dieValues,
            addToTotal = 0,
            adjusted = listOf(adjusted)
        )
        
        // Initial value
        assertEquals(3, testDie1.value)
        
        // Act
        SUT(player, combination)
        
        // Assert
        assertEquals(6, player.diceInHand[0]?.value) // Maximum value for a d6
        assertTrue(player.delayedEffectList.isEmpty()) // Effect should be removed
    }
    
    @Test
    fun invoke_withMultipleAdjustments_processesAllAdjustments() {
        // Arrange
        val adjustment1 = 2
        val adjustEffect1 = AppliedEffect.AdjustDieRoll(adjustment1)
        val adjustEffect2 = AppliedEffect.AdjustDieToMax()
        player.delayedEffectList.addAll(listOf(adjustEffect1, adjustEffect2))
        
        val adjusted1 = Adjusted.ByAmount(testDie1, adjustment1)
        val adjusted2 = Adjusted.ToMax(testDie2)
        val dieValues = DieValues(listOf(testDie1, testDie2))
        val combination = Combination(
            values = dieValues,
            addToTotal = 0,
            adjusted = listOf(adjusted1, adjusted2)
        )
        
        // Initial values
        assertEquals(3, testDie1.value)
        assertEquals(5, testDie2.value)
        
        // Act
        SUT(player, combination)
        
        // Assert
        val die1 = player.diceInHand.dice[0]
        val die2 = player.diceInHand.dice[1]
        assertEquals(5, die1.value) // 3 + 2 = 5
        assertEquals(8, die2.value) // Maximum value for a d8
        assertTrue(player.delayedEffectList.isEmpty()) // Both effects should be removed
    }
    
    @Test
    fun invoke_withoutMatchingEffect_adjustsDieButDoesntRemoveEffect() {
        // Arrange - using an adjustment amount different from the effect
        val effectAmount = 3
        val adjustmentAmount = 2
        val adjustEffect = AppliedEffect.AdjustDieRoll(effectAmount) // Different amount
        player.delayedEffectList.addAll(listOf(adjustEffect))
        
        val adjusted = Adjusted.ByAmount(testDie1, adjustmentAmount)
        val dieValues = DieValues(listOf(testDie1, testDie2))
        val combination = Combination(
            values = dieValues,
            addToTotal = 0,
            adjusted = listOf(adjusted)
        )
        
        // Act
        SUT(player, combination)
        
        // Assert
        assertEquals(5, player.diceInHand[0]?.value) // 3 + 2 = 5
        assertFalse(player.delayedEffectList.isEmpty()) // Effect should not be removed
        assertEquals(adjustEffect, player.delayedEffectList.copy()[0]) // Original effect still there
    }
    
    @Test
    fun invoke_withNoEffectsInPlayer_stillAdjustsDie() {
        // Arrange - no effects in player's list
        val adjustment = 2
        
        val adjusted = Adjusted.ByAmount(testDie1, adjustment)
        val dieValues = DieValues(listOf(testDie1, testDie2))
        val combination = Combination(
            values = dieValues,
            addToTotal = 0,
            adjusted = listOf(adjusted)
        )
        
        // Act
        SUT(player, combination)
        
        // Assert
        assertEquals(5, player.diceInHand[0]?.value) // 3 + 2 = 5
        assertTrue(player.delayedEffectList.isEmpty()) // Still empty
    }
    
    @Test
    fun invoke_withNoAdjustments_doesNothing() {
        // Arrange
        val adjustEffect = AppliedEffect.AdjustDieRoll(2)
        player.delayedEffectList.addAll(listOf(adjustEffect))
        
        val dieValues = DieValues(listOf(testDie1, testDie2))
        val combination = Combination(
            values = dieValues,
            addToTotal = 0,
            adjusted = emptyList() // No adjustments
        )
        
        // Initial values
        assertEquals(3, testDie1.value)
        assertEquals(5, testDie2.value)
        
        // Act
        SUT(player, combination)
        
        // Assert
        assertEquals(3, testDie1.value) // Unchanged
        assertEquals(5, testDie2.value) // Unchanged
        assertFalse(player.delayedEffectList.isEmpty()) // Effects unchanged
    }
} 
