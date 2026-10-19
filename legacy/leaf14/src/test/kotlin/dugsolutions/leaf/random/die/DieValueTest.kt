package dugsolutions.leaf.random.die

import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.Randomizer
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DieValueTest {
    companion object {
        private const val TEST_SIDES = 6
        private const val TEST_VALUE = 3
    }
    private lateinit var randomizer: Randomizer
    private lateinit var dieFactory: DieFactory
    private lateinit var sampleDie: SampleDie

    private lateinit var SUT: DieValue

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        dieFactory = DieFactory(randomizer)
        sampleDie = SampleDie(randomizer)

        SUT = DieValue(TEST_SIDES, TEST_VALUE)
    }

    @Test
    fun whenCreated_hasCorrectSidesAndValue() {
        // Assert
        assertEquals(TEST_SIDES, SUT.sides, "DieValue should have correct sides")
        assertEquals(TEST_VALUE, SUT.value, "DieValue should have correct value")
    }
    
    @Test
    fun whenValueExceedsSides_isConstrainedToSides() {
        // Arrange
        val value = TEST_SIDES + 2
        val dieValue = DieValue(TEST_SIDES, value)
        
        // Assert
        assertEquals(TEST_SIDES, dieValue.value, "DieValue should be constrained to sides")
    }
    
    @Test
    fun whenValueBelowOne_isConstrainedToOne() {
        // Arrange
        val value = 0
        val dieValue = DieValue(TEST_SIDES, value)
        
        // Assert
        assertEquals(1, dieValue.value, "DieValue should be constrained to minimum of 1")
    }

    @Test
    fun adjustTo_returnsSameInstance() {
        // Act
        val result = SUT.adjustTo(4)
        
        // Assert
        assertSame(SUT, result, "adjustTo should return same instance")
        assertEquals(4, SUT.value, "Value should be updated")
    }
    
    @Test
    fun adjustBy_returnsSameInstance() {
        // Act
        val result = SUT.adjustBy(2)
        
        // Assert
        assertSame(SUT, result, "adjustBy should return same instance")
        assertEquals(5, SUT.value, "Value should be updated")
    }
    
    @Test
    fun adjustToMax_returnsCorrectAmount() {
        // Act
        val amount = SUT.adjustToMax()
        
        // Assert
        assertEquals(TEST_SIDES, amount, "Should return correct amount to reach max")
        assertEquals(TEST_SIDES, SUT.value, "Value should be set to max")
    }

    @Test
    fun dieFrom_createsDieWithCorrectSidesAndValue() {
        // Act
        val die = SUT.dieFrom(dieFactory)
        
        // Assert
        assertEquals(TEST_SIDES, die.sides, "Created die should have correct sides")
        assertEquals(TEST_VALUE, die.value, "Created die should have correct value")
    }

    @Test
    fun equals_whenSameSidesAndValue_returnsTrue() {
        // Arrange
        val otherDieValue = DieValue(TEST_SIDES, TEST_VALUE)
        
        // Assert
        assertEquals(SUT, otherDieValue)
        assertEquals(otherDieValue, SUT)
    }

    @Test
    fun equals_whenDifferentSides_returnsFalse() {
        // Arrange
        val otherDieValue = DieValue(4, TEST_VALUE)
        
        // Assert
        assertNotEquals(SUT, otherDieValue)
        assertNotEquals(otherDieValue, SUT)
    }

    @Test
    fun equals_whenDifferentValues_returnsFalse() {
        // Arrange
        val otherDieValue = DieValue(TEST_SIDES, 4)
        
        // Assert
        assertNotEquals(SUT, otherDieValue)
        assertNotEquals(otherDieValue, SUT)
    }

    @Test
    fun equals_whenComparedToDieWithSameSidesAndValue_returnsTrue() {
        // Arrange
        val die = sampleDie.d6
        die.adjustTo(TEST_VALUE)
        
        // Assert
        assertEquals(SUT as DieBase, die)
        assertEquals(die as DieBase, SUT)
    }

    @Test
    fun equals_whenComparedToDieWithDifferentSides_returnsFalse() {
        // Arrange
        val die = sampleDie.d4
        die.adjustTo(TEST_VALUE)
        
        // Assert
        assertNotEquals(SUT, die)
        assertNotEquals(die, SUT)
    }

    @Test
    fun equals_whenComparedToDieWithDifferentValue_returnsFalse() {
        // Arrange
        val die = sampleDie.d6
        die.adjustTo(4)
        
        // Assert
        assertNotEquals(SUT, die)
        assertNotEquals(die, SUT)
    }

    @Test
    fun equals_whenComparedToNull_returnsFalse() {
        // Assert
        assertNotEquals(null, SUT)
        assertNotEquals(SUT, null)
    }

    @Test
    fun equals_whenComparedToDifferentType_returnsFalse() {
        // Arrange
        val otherObject = "not a die"
        
        // Assert
        assertNotEquals(SUT, otherObject)
        assertNotEquals(otherObject, SUT)
    }

    @Test
    fun hashCode_whenSameSidesAndValue_returnsSameHash() {
        // Arrange
        val otherDieValue = DieValue(TEST_SIDES, TEST_VALUE)
        
        // Assert
        assertEquals(SUT.hashCode(), otherDieValue.hashCode())
    }

    @Test
    fun hashCode_whenDifferentSides_returnsDifferentHash() {
        // Arrange
        val otherDieValue = DieValue(4, TEST_VALUE)
        
        // Assert
        assertNotEquals(SUT.hashCode(), otherDieValue.hashCode())
    }

    @Test
    fun hashCode_whenDifferentValues_returnsDifferentHash() {
        // Arrange
        val otherDieValue = DieValue(TEST_SIDES, 4)
        
        // Assert
        assertNotEquals(SUT.hashCode(), otherDieValue.hashCode())
    }

    @Test
    fun toString_returnsCorrectFormat() {
        // Act
        val result = SUT.toString()
        
        // Assert
        assertEquals("DieValue(sides=$TEST_SIDES, value=$TEST_VALUE)", result)
    }
} 
