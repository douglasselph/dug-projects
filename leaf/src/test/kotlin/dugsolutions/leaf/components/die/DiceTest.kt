package dugsolutions.leaf.components.die

import dugsolutions.leaf.tool.Randomizer
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiceTest {

    private lateinit var SUT: Dice
    private lateinit var sampleDie: SampleDie
    private lateinit var randomizer: Randomizer

    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die
    private lateinit var d10: Die
    private lateinit var d12: Die

    @BeforeEach
    fun setup() {
        // Initialize random components
        randomizer = Randomizer.create()
        sampleDie = SampleDie(randomizer)

        // Create test dice with different sides
        d4 = sampleDie.d4
        d6 = sampleDie.d6
        d8 = sampleDie.d8
        d10 = sampleDie.d10
        d12 = sampleDie.d12

        // Initialize empty dice collection
        SUT = Dice()
    }

    @Test
    fun size_whenEmpty_returnsZero() {
        assertEquals(0, SUT.size)
    }

    @Test
    fun size_whenPopulated_returnsCorrectCount() {
        SUT.add(d6)
        SUT.add(d8)
        assertEquals(2, SUT.size)
    }

    @Test
    fun isEmpty_whenEmpty_returnsTrue() {
        assertTrue(SUT.isEmpty())
    }

    @Test
    fun isEmpty_whenPopulated_returnsFalse() {
        SUT.add(d6)
        assertFalse(SUT.isEmpty())
    }

    @Test
    fun isNotEmpty_whenEmpty_returnsFalse() {
        assertFalse(SUT.isNotEmpty())
    }

    @Test
    fun isNotEmpty_whenPopulated_returnsTrue() {
        SUT.add(d6)
        assertTrue(SUT.isNotEmpty())
    }

    @Test
    fun draw_whenEmpty_returnsNull() {
        assertNull(SUT.draw())
    }

    @Test
    fun draw_whenPopulated_returnsLowestSidedDie() {
        SUT.add(d8)
        SUT.add(d4)
        SUT.add(d12)
        
        val result = SUT.draw()
        assertEquals(d4, result)
        assertEquals(2, SUT.size)
    }

    @Test
    fun drawHighest_whenEmpty_returnsNull() {
        assertNull(SUT.drawHighest())
    }

    @Test
    fun drawHighest_whenPopulated_returnsHighestSidedDie() {
        SUT.add(d4)
        SUT.add(d12)
        SUT.add(d6)
        
        val result = SUT.drawHighest()
        assertEquals(d12, result)
        assertEquals(2, SUT.size)
    }

    @Test
    fun drawLowest_whenEmpty_returnsNull() {
        assertNull(SUT.drawLowest())
    }

    @Test
    fun drawLowest_whenPopulated_returnsLowestSidedDie() {
        SUT.add(d8)
        SUT.add(d4)
        SUT.add(d12)
        SUT.sort()

        val result = SUT.drawLowest()
        assertEquals(d4, result)
        assertEquals(2, SUT.size)
    }

    @Test
    fun add_addsDieAndMaintainsSort() {
        // Arrange
        SUT.add(d8)
        SUT.add(d4)
        SUT.add(d12)
        SUT.sort()

        // Assert
        assertEquals(3, SUT.size)
        assertEquals(d4, SUT.dice[0])
        assertEquals(d8, SUT.dice[1])
        assertEquals(d12, SUT.dice[2])
    }

    @Test
    fun addAll_addsMultipleDiceAndMaintainsSort() {
        SUT.addAll(listOf(d8, d4, d12, d6)).sort()
        
        assertEquals(4, SUT.size)
        assertEquals(d4, SUT.dice[0])
        assertEquals(d6, SUT.dice[1])
        assertEquals(d8, SUT.dice[2])
        assertEquals(d12, SUT.dice[3])
    }

    @Test
    fun clear_removesAllDice() {
        SUT.add(d6)
        SUT.add(d8)
        SUT.clear()
        
        assertTrue(SUT.isEmpty())
    }

    @Test
    fun plus_combinesTwoDiceCollections() {
        val dice1 = Dice(listOf(d4, d6))
        val dice2 = Dice(listOf(d8, d12))
        
        val result = dice1 + dice2
        
        assertEquals(4, result.size)
        assertEquals(d4, result.dice[0])
        assertEquals(d6, result.dice[1])
        assertEquals(d8, result.dice[2])
        assertEquals(d12, result.dice[3])
    }

    @Test
    fun remove_whenDieExists_returnsTrueAndRemovesDie() {
        SUT.add(d6)
        SUT.add(d8)
        
        val result = SUT.remove(d6)
        
        assertTrue(result)
        assertEquals(1, SUT.size)
        assertEquals(d8, SUT.dice[0])
    }

    @Test
    fun remove_whenDieDoesNotExist_returnsFalse() {
        SUT.add(d6)
        SUT.add(d8)
        
        val result = SUT.remove(d10)
        
        assertFalse(result)
        assertEquals(2, SUT.size)
    }

    @Test
    fun adjust_whenDieExists_adjustsValue() {
        d6.adjustTo(4)
        d8.adjustTo(6)
        SUT.add(d6)
        SUT.add(d8)
        
        val result = SUT.adjust(d6, 2)
        
        assertTrue(result)
        assertEquals(6, d6.value)
    }

    @Test
    fun adjust_whenDieDoesNotExist_returnsFalse() {
        SUT.add(d6)
        SUT.add(d8)
        
        val result = SUT.adjust(d10, 2)
        
        assertFalse(result)
    }

    @Test
    fun adjustToMax_whenDieExists_setsValueToMax() {
        SUT.add(d6)
        SUT.add(d8)
        
        val result = SUT.adjustToMax(d6)
        
        assertTrue(result)
        assertEquals(6, d6.value)
    }

    @Test
    fun adjustToMax_whenDieDoesNotExist_returnsFalse() {
        SUT.add(d6)
        SUT.add(d8)
        
        val result = SUT.adjustToMax(d10)
        
        assertFalse(result)
    }

    @Test
    fun values_whenPopulated_returnsFormattedValues() {
        // Arrange
        d4.adjustTo(3)
        d6.adjustTo(5)
        d8.adjustTo(7)
        SUT.addAll(listOf(d4, d6, d8))

        // Act
        val result = SUT.values()

        // Assert
        assertEquals("D4(3),D6(5),D8(7)", result)
    }

    @Test
    fun values_whenMultipleSameSidedDice_returnsAllValues() {
        // Arrange
        val d4_2 = sampleDie.d4
        d4.adjustTo(2)
        d4_2.adjustTo(4)
        SUT.addAll(listOf(d4, d4_2))

        // Act
        val result = SUT.values()

        // Assert
        assertEquals("D4(2),D4(4)", result)
    }

    @Test
    fun toString_whenEmpty_returnsEmptyString() {
        assertEquals("", SUT.toString())
    }

    @Test
    fun toString_whenSingleDie_returnsCorrectFormat() {
        // Arrange
        SUT.add(d6)

        // Act
        val result = SUT.toString()

        // Assert
        assertEquals("1D6", result)
    }

    @Test
    fun toString_whenMultipleDifferentDice_returnsSortedFormat() {
        // Arrange
        SUT.addAll(listOf(d8, d4, d12, d6))

        // Act
        val result = SUT.toString()

        // Assert
        assertEquals("1D4,1D6,1D8,1D12", result)
    }

    @Test
    fun toString_whenMultipleSameSidedDice_returnsGroupedFormat() {
        // Arrange
        val d4_2 = sampleDie.d4
        val d6_2 = sampleDie.d6
        SUT.addAll(listOf(d4, d4_2, d6, d6_2))

        // Act
        val result = SUT.toString()

        // Assert
        assertEquals("2D4,2D6", result)
    }

    @Test
    fun totalSides_whenEmpty_returnsZero() {
        assertEquals(0, SUT.totalSides)
    }

    @Test
    fun totalSides_whenPopulated_returnsSumOfSides() {
        // Arrange
        SUT.addAll(listOf(d4, d6, d8))

        // Act
        val result = SUT.totalSides

        // Assert
        assertEquals(18, result) // 4 + 6 + 8 = 18
    }

    @Test
    fun copy_whenEmpty_returnsEmptyList() {
        assertTrue(SUT.copy.isEmpty())
    }

    @Test
    fun copy_whenPopulated_returnsListOfDieValues() {
        // Arrange
        d4.adjustTo(3)
        d6.adjustTo(5)
        SUT.addAll(listOf(d4, d6))

        // Act
        val result = SUT.copy

        // Assert
        assertEquals(2, result.size)
        assertEquals(3, result[0].value)
        assertEquals(5, result[1].value)
    }

    @Test
    fun hasDie_whenDieExists_returnsTrue() {
        // Arrange
        SUT.add(d6)
        SUT.add(d8)

        // Act & Assert
        assertTrue(SUT.hasDie(d6))
    }

    @Test
    fun hasDie_whenDieDoesNotExist_returnsFalse() {
        // Arrange
        SUT.add(d6)
        SUT.add(d8)

        // Act & Assert
        assertFalse(SUT.hasDie(d10))
    }

    @Test
    fun hasDie_whenDieValueExists_returnsTrue() {
        // Arrange
        d6.adjustTo(4)
        SUT.add(d6)
        SUT.add(d8)

        // Act & Assert
        assertTrue(SUT.hasDie(DieValue(6, 4)))
    }

    @Test
    fun hasDie_whenDieValueDoesNotExist_returnsFalse() {
        // Arrange
        SUT.add(d6)
        SUT.add(d8)

        // Act & Assert
        assertFalse(SUT.hasDie(DieValue(10, 5)))
    }

    @Test
    fun remove_whenDieValueExists_returnsTrueAndRemovesDie() {
        // Arrange
        d6.adjustTo(4)
        SUT.add(d6)
        SUT.add(d8)

        // Act
        val result = SUT.remove(DieValue(6, 4))

        // Assert
        assertTrue(result)
        assertEquals(1, SUT.size)
        assertEquals(d8, SUT.dice[0])
    }

    @Test
    fun remove_whenDieValueDoesNotExist_returnsFalse() {
        // Arrange
        SUT.add(d6)
        SUT.add(d8)

        // Act
        val result = SUT.remove(DieValue(10, 5))

        // Assert
        assertFalse(result)
        assertEquals(2, SUT.size)
    }

    @Test
    fun adjust_whenDieValueExists_adjustsValue() {
        // Arrange
        d6.adjustTo(4)
        SUT.add(d6)
        SUT.add(d8)

        // Act
        val result = SUT.adjust(DieValue(6, 4), 2)

        // Assert
        assertTrue(result)
        assertEquals(6, d6.value)
    }

    @Test
    fun adjust_whenDieValueDoesNotExist_returnsFalse() {
        // Arrange
        SUT.add(d6)
        SUT.add(d8)

        // Act
        val result = SUT.adjust(DieValue(10, 5), 2)

        // Assert
        assertFalse(result)
    }

    @Test
    fun adjustToMax_whenDieValueExists_setsValueToMax() {
        // Arrange
        d6.adjustTo(4)
        SUT.add(d6)
        SUT.add(d8)

        // Act
        val result = SUT.adjustToMax(DieValue(6, 4))

        // Assert
        assertTrue(result)
        assertEquals(6, d6.value)
    }

    @Test
    fun adjustToMax_whenDieValueDoesNotExist_returnsFalse() {
        // Arrange
        SUT.add(d6)
        SUT.add(d8)

        // Act
        val result = SUT.adjustToMax(DieValue(10, 5))

        // Assert
        assertFalse(result)
    }

    @Test
    fun values_whenEmpty_returnsEmptyString() {
        // Act
        val result = SUT.values()

        // Assert
        assertEquals("", result)
    }

    @Test
    fun get_whenIndexInBounds_returnsDie() {
        // Arrange
        SUT.addAll(listOf(d4, d6, d8))

        // Act & Assert
        assertEquals(d4, SUT[0])
        assertEquals(d6, SUT[1])
        assertEquals(d8, SUT[2])
    }

    @Test
    fun get_whenIndexOutOfBounds_returnsNull() {
        // Arrange
        SUT.addAll(listOf(d4, d6, d8))

        // Act & Assert
        assertNull(SUT[-1])
        assertNull(SUT[3])
    }

    @Test
    fun get_whenEmpty_returnsNull() {
        // Act & Assert
        assertNull(SUT[0])
    }
} 
