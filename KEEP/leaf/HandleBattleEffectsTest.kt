package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.game.turn.select.SelectDieToMax
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.effect.AppliedEffect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandleBattleEffectsTest {

    private lateinit var mockGameChronicle: GameChronicle
    private lateinit var mockSelectDieToAdjust: SelectDieToAdjust
    private lateinit var mockSelectDieToMax: SelectDieToMax
    private lateinit var player: PlayerTD
    private lateinit var sampleDie: SampleDie
    
    private lateinit var SUT: HandleBattleEffects

    @BeforeEach
    fun setup() {
        sampleDie = SampleDie(PlayerTD.randomizerTD)
        mockGameChronicle = mockk(relaxed = true)
        mockSelectDieToAdjust = mockk(relaxed = true)
        mockSelectDieToMax = mockk(relaxed = true)
        
        player = PlayerTD("Test Player", 1)
        // Clear any existing effects
        player.effectsList.clear()
        
        SUT = HandleBattleEffects(
            mockGameChronicle,
            mockSelectDieToAdjust,
            mockSelectDieToMax
        )
    }

    @Test
    fun invoke_whenAdjustDieRollEffect_adjustsSelectedDie() {
        // Arrange
        val testDie = sampleDie.d6.adjustTo(2)
        player.diceInHand.clear()
        player.addDieToHand(testDie)
        
        val adjustmentAmount = 2
        val adjustDieRollEffect = AppliedEffect.AdjustDieRoll(adjustmentAmount)
        player.effectsList.addAll(listOf(adjustDieRollEffect))
        
        // Mock the die selection
        every { mockSelectDieToAdjust(player.diceInHand, adjustmentAmount) } returns testDie
        
        // Act
        SUT(player)
        
        // Assert
        assertEquals(4, testDie.value) // 2 + 2 = 4
        verify { mockGameChronicle(any<GameChronicle.Moment.ADJUST_DIE>()) }
        assertTrue(player.effectsList.copy().isEmpty()) // Effect should be removed
    }
    
    @Test
    fun invoke_whenAdjustDieRollEffect_andNoValidDie_doesNotAdjust() {
        // Arrange
        val adjustmentAmount = 2
        val adjustDieRollEffect = AppliedEffect.AdjustDieRoll(adjustmentAmount)
        player.effectsList.addAll(listOf(adjustDieRollEffect))
        
        // Mock the die selection to return null (no valid die)
        every { mockSelectDieToAdjust(player.diceInHand, adjustmentAmount) } returns null
        
        // Act
        SUT(player)
        
        // Assert
        verify(exactly = 0) { mockGameChronicle(any<GameChronicle.Moment.ADJUST_DIE>()) }
        assertTrue(player.effectsList.copy().isEmpty()) // Effect should still be removed even if not used
    }
    
    @Test
    fun invoke_whenAdjustDieToMaxEffect_adjustsDieToMaximum() {
        // Arrange
        val testDie = sampleDie.d6.adjustTo(3)
        player.diceInHand.clear()
        player.addDieToHand(testDie)
        
        val adjustToMaxEffect = AppliedEffect.AdjustDieToMax()
        player.effectsList.addAll(listOf(adjustToMaxEffect))
        
        // Mock the die selection
        every { mockSelectDieToMax(player.diceInHand) } returns testDie
        
        // Act
        SUT(player)
        
        // Assert
        assertEquals(6, testDie.value) // Adjusted to max (6)
        verify { mockGameChronicle(any<GameChronicle.Moment.ADJUST_DIE>()) }
        assertTrue(player.effectsList.copy().isEmpty()) // Effect should be removed
    }
    
    @Test
    fun invoke_whenAdjustDieToMaxEffect_andNoValidDie_doesNotAdjust() {
        // Arrange
        val adjustToMaxEffect = AppliedEffect.AdjustDieToMax()
        player.effectsList.addAll(listOf(adjustToMaxEffect))
        
        // Mock the die selection to return null (no valid die)
        every { mockSelectDieToMax(player.diceInHand) } returns null
        
        // Act
        SUT(player)
        
        // Assert
        verify(exactly = 0) { mockGameChronicle(any<GameChronicle.Moment.ADJUST_DIE>()) }
        assertTrue(player.effectsList.copy().isEmpty()) // Effect should still be removed even if not used
    }
    
    @Test
    fun invoke_whenAddToTotalEffect_increasesPipModifier() {
        // Arrange
        val initialPipModifier = 2
        val additionalAmount = 3
        
        player.pipModifier = initialPipModifier
        val addToTotalEffect = AppliedEffect.AddToTotal(additionalAmount)
        player.effectsList.addAll(listOf(addToTotalEffect))
        
        // Act
        SUT(player)
        
        // Assert
        assertEquals(initialPipModifier + additionalAmount, player.pipModifier)
        verify { mockGameChronicle(any<GameChronicle.Moment.ADD_TO_TOTAL>()) }
        assertTrue(player.effectsList.copy().isEmpty()) // Effect should be removed
    }
    
    @Test
    fun invoke_whenUnsupportedEffect_doesNotRemoveEffect() {
        // Arrange
        val unsupportedEffect = AppliedEffect.DrawCards(2)
        player.effectsList.addAll(listOf(unsupportedEffect))
        
        // Act
        SUT(player)
        
        // Assert
        assertEquals(1, player.effectsList.copy().size) // Effect should NOT be removed
        assertEquals(unsupportedEffect, player.effectsList.copy()[0]) // The unsupported effect should remain
        verify(exactly = 0) { mockGameChronicle(any()) }
    }
    
    @Test
    fun invoke_whenMultipleEffects_handlesEachCorrectly() {
        // Arrange
        val testDie = sampleDie.d6.adjustTo(3)
        player.diceInHand.clear()
        player.addDieToHand(testDie)
        
        val initialPipModifier = 2
        player.pipModifier = initialPipModifier
        
        val adjustDieRollEffect = AppliedEffect.AdjustDieRoll(1)
        val addToTotalEffect = AppliedEffect.AddToTotal(3)
        val unsupportedEffect = AppliedEffect.DrawCards(2)
        
        player.effectsList.addAll(listOf(adjustDieRollEffect, addToTotalEffect, unsupportedEffect))
        
        // Mock the die selection
        every { mockSelectDieToAdjust(player.diceInHand, 1) } returns testDie
        
        // Act
        SUT(player)
        
        // Assert
        assertEquals(4, testDie.value) // 3 + 1 = 4
        assertEquals(initialPipModifier + 3, player.pipModifier)
        
        // Only unsupported effect should remain
        val remainingEffects = player.effectsList.copy()
        assertEquals(1, remainingEffects.size)
        assertEquals(unsupportedEffect, remainingEffects[0])
        
        verify(exactly = 1) { mockGameChronicle(ofType<GameChronicle.Moment.ADJUST_DIE>()) }
        verify(exactly = 1) { mockGameChronicle(ofType<GameChronicle.Moment.ADD_TO_TOTAL>()) }
    }
} 
