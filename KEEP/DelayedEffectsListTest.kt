package dugsolutions.leaf.player.effect

import dugsolutions.leaf.player.domain.AppliedEffect
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DelayedEffectsListTest {

    companion object {
        private const val ADJUSTMENT_AMOUNT = 3
    }

    private lateinit var mockEffect1: AppliedEffect
    private lateinit var mockEffect2: AppliedEffect
    private lateinit var adjustDieRollEffect: AppliedEffect.AdjustDieRoll
    private lateinit var adjustToMaxEffect: AppliedEffect.AdjustDieToMax

    private lateinit var SUT: DelayedEffectsList

    @BeforeEach
    fun setup() {
        SUT = DelayedEffectsList()
        mockEffect1 = AppliedEffect.DrawCards(2)
        mockEffect2 = AppliedEffect.DrawDice(1)
        adjustDieRollEffect = AppliedEffect.AdjustDieRoll(ADJUSTMENT_AMOUNT)
        adjustToMaxEffect = AppliedEffect.AdjustDieToMax()
    }

    @Test
    fun clear_removesAllEffects() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, mockEffect2))

        // Act
        SUT.clear()

        // Assert
        assertTrue(SUT.isEmpty())
    }

    @Test
    fun addAll_addsMultipleEffects() {
        // Act
        SUT.addAll(listOf(mockEffect1, mockEffect2))

        // Assert
        assertEquals(2, SUT.toList().size)
        assertTrue(SUT.toList().contains(mockEffect1))
        assertTrue(SUT.toList().contains(mockEffect2))
    }

    @Test
    fun remove_removesEffect() {
        // Arrange
        SUT.addAll(listOf(mockEffect1))

        // Act
        SUT.remove(mockEffect1)

        // Assert
        assertTrue(SUT.isEmpty())
    }

    @Test
    fun removeAll_removesMultipleEffects() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, mockEffect2))

        // Act
        SUT.removeAll(listOf(mockEffect1, mockEffect2))

        // Assert
        assertTrue(SUT.isEmpty())
    }

    @Test
    fun filterAndRemove_removesMatchingEffects() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, mockEffect2))

        // Act
        val removedEffects = SUT.filterAndRemove { it is AppliedEffect.DrawCards }

        // Assert
        assertEquals(1, removedEffects.size)
        assertEquals(mockEffect1, removedEffects.first())
        assertEquals(1, SUT.toList().size)
        assertEquals(mockEffect2, SUT.toList().first())
    }

    @Test
    fun copy_returnsNewListWithSameEffects() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, mockEffect2))

        // Act
        val copy = SUT.copy()

        // Assert
        assertEquals(2, copy.size)
        assertTrue(copy.contains(mockEffect1))
        assertTrue(copy.contains(mockEffect2))
    }

    @Test
    fun isEmpty_whenEmpty_returnsTrue() {
        assertTrue(SUT.isEmpty())
    }

    @Test
    fun isEmpty_whenNotEmpty_returnsFalse() {
        // Arrange
        SUT.addAll(listOf(mockEffect1))

        // Assert
        assertFalse(SUT.isEmpty())
    }

    @Test
    fun iterator_iteratesOverAllEffects() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, mockEffect2))

        // Act
        val effects = SUT.toList()

        // Assert
        assertEquals(2, effects.size)
        assertTrue(effects.contains(mockEffect1))
        assertTrue(effects.contains(mockEffect2))
    }
    
    @Test
    fun findAdjustDieRoll_whenExists_returnsMatchingEffect() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, adjustDieRollEffect, mockEffect2))
        
        // Act
        val result = SUT.findAdjustDieRoll(ADJUSTMENT_AMOUNT)
        
        // Assert
        assertEquals(adjustDieRollEffect, result)
    }
    
    @Test
    fun findAdjustDieRoll_whenDoesNotExist_returnsNull() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, mockEffect2))
        
        // Act
        val result = SUT.findAdjustDieRoll(ADJUSTMENT_AMOUNT)
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun findAdjustDieRoll_whenDifferentAmount_returnsNull() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, adjustDieRollEffect, mockEffect2))
        
        // Act
        val result = SUT.findAdjustDieRoll(ADJUSTMENT_AMOUNT + 1)
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun findAdjustToMax_whenExists_returnsMatchingEffect() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, adjustToMaxEffect, mockEffect2))
        
        // Act
        val result = SUT.findAdjustToMax()
        
        // Assert
        assertEquals(adjustToMaxEffect, result)
    }
    
    @Test
    fun findAdjustToMax_whenDoesNotExist_returnsNull() {
        // Arrange
        SUT.addAll(listOf(mockEffect1, mockEffect2))
        
        // Act
        val result = SUT.findAdjustToMax()
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun findAdjustToMax_returnsFirstWhenMultipleExist() {
        // Arrange
        val adjustToMaxEffect2 = AppliedEffect.AdjustDieToMax()
        SUT.addAll(listOf(mockEffect1, adjustToMaxEffect, adjustToMaxEffect2))
        
        // Act
        val result = SUT.findAdjustToMax()
        
        // Assert
        assertEquals(adjustToMaxEffect, result)
    }
} 
