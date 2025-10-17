package dugsolutions.leaf.random

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RandomizerTDTest {

    private lateinit var randomizer: RandomizerTD

    @BeforeEach
    fun setup() {
        randomizer = RandomizerTD()
    }

    @Test
    fun setValues_controlsNextIntValues() {
        // Arrange
        val testValues = listOf(42, 13, 7)
        
        // Act
        randomizer.setValues(testValues)
        
        // Assert
        assertEquals(42, randomizer.nextInt(100))
        assertEquals(13, randomizer.nextInt(100))
        assertEquals(7, randomizer.nextInt(100))
        // After reaching the end, it should cycle back
        assertEquals(42, randomizer.nextInt(100))
    }
    
    @Test
    fun setValues_controlsNextIntWithRange() {
        // Arrange
        val testValues = listOf(5, 10, 15)
        
        // Act
        randomizer.setValues(testValues)
        
        // Assert
        // For range (3, 7), with value 5:
        // v1 = 5 - 3 = 2
        // v2 = 2 % (7 - 3) = 2
        // result = 2 + 3 = 5
        assertEquals(5, randomizer.nextInt(3, 7))
        
        // For range (2, 8), with value 10:
        // v1 = 10 - 2 = 8
        // v2 = 8 % (8 - 2) = 2
        // result = 2 + 2 = 4
        assertEquals(4, randomizer.nextInt(2, 8))
        
        // For range (1, 5), with value 15:
        // v1 = 15 - 1 = 14
        // v2 = 14 % (5 - 1) = 2
        // result = 2 + 1 = 3
        assertEquals(3, randomizer.nextInt(1, 5))
    }
    
    @Test
    fun setValues_controlsNextBoolean() {
        // Arrange
        val testValues = listOf(1, 0, 5)
        
        // Act
        randomizer.setValues(testValues)
        
        // Assert
        assertTrue(randomizer.nextBoolean()) // 1 != 0 -> true
        assertFalse(randomizer.nextBoolean()) // 0 != 0 -> false
        assertTrue(randomizer.nextBoolean()) // 5 != 0 -> true
    }
    
    @Test
    fun setValues_resetsCurrentIndex() {
        // Arrange
        val initialValues = listOf(1, 2, 3)
        randomizer.setValues(initialValues)
        
        // Consume two values
        randomizer.nextInt(10)
        randomizer.nextInt(10)
        
        // Act - Set new values
        val newValues = listOf(42, 43, 44)
        randomizer.setValues(newValues)
        
        // Assert - Should start from the beginning of new values
        assertEquals(42, randomizer.nextInt(100))
        assertEquals(43, randomizer.nextInt(100))
    }
    
    @Test
    fun setValues_affectsShuffled() {
        // Arrange
        val testList = listOf("A", "B", "C", "D")
        
        // Test with shift of 2
        randomizer.setValues(listOf(2))
        
        // Act 
        val result1 = randomizer.shuffled(testList)
        
        // Assert
        // With shift=2, the result should be ["C", "D", "A", "B"]
        assertEquals(listOf("C", "D", "A", "B"), result1)
        
        // Test with shift of 1
        randomizer.setValues(listOf(1))
        
        // Act
        val result2 = randomizer.shuffled(testList)
        
        // Assert
        // With shift=1, the result should be ["B", "C", "D", "A"]
        assertEquals(listOf("B", "C", "D", "A"), result2)
    }
    
    @Test
    fun setValues_affectsRandomOrNull() {
        // Arrange
        val testList = listOf("A", "B", "C", "D")
        
        // Set randomOrNullIndex
        randomizer.randomOrNullIndex = 2
        
        // Act & Assert
        assertEquals("C", randomizer.randomOrNull(testList))
        
        // Change randomOrNullIndex
        randomizer.randomOrNullIndex = 1
        
        // Act & Assert
        assertEquals("B", randomizer.randomOrNull(testList))
    }
    
    @Test
    fun setValues_emptyList_cyclesCorrectly() {
        // Arrange
        randomizer.setValues(emptyList())
        
        // Act & Assert
        // Default values should be used
        assertEquals(0, randomizer.nextInt(100))
    }
    
    @Test
    fun cyclesCorrectly() {
        // Arrange
        val testValues = listOf(5, 10)
        randomizer.setValues(testValues)
        
        // Act & Assert
        assertEquals(5, randomizer.nextInt(100))
        assertEquals(10, randomizer.nextInt(100))
        // After reaching the end, it should cycle back
        assertEquals(5, randomizer.nextInt(100))
        assertEquals(10, randomizer.nextInt(100))
    }
} 
