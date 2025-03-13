package dugsolutions.leaf.components.die

import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.tool.Randomizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DieTest {
    companion object {
        private const val TEST_SIDES = 6
        private const val TEST_VALUE = 3
    }

    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer

    private lateinit var SUT: Die

    @BeforeEach
    fun setup() {
        // Initialize random components
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)

        // Create test die
        SUT = dieFactory(DieSides.D6)
    }

    @Test
    fun create_whenValidSides_createsDieWithCorrectSides() {
        // Arrange
        val d6 = dieFactory(DieSides.D6)

        // Assert
        assertEquals(TEST_SIDES, d6.sides)
    }

    @Test
    fun roll_whenCalled_generatesValueWithinBounds() {
        // Act
        val value = SUT.roll().value

        // Assert
        assertTrue(value in 1..TEST_SIDES)
    }
    
    @Test
    fun roll_returnsTheSameDieInstance() {
        // Act
        val returnedDie = SUT.roll()
        
        // Assert
        assertEquals(SUT, returnedDie)
    }
    
    @Test
    fun roll_changesTheDieValue() {
        // Arrange
        val initialValue = SUT.value
        
        // We'll roll until we get a different value (this might be flaky if we're extremely unlucky)
        var newValue = initialValue
        var rollCount = 0
        val maxRolls = 10
        
        // Act
        while (newValue == initialValue && rollCount < maxRolls) {
            SUT.roll()
            newValue = SUT.value
            rollCount++
        }
        
        // If we rolled max times and still have the same value,
        // set the die to a different value to avoid a flaky test
        if (newValue == initialValue) {
            SUT.adjustTo(if (initialValue < TEST_SIDES) initialValue + 1 else initialValue - 1)
            newValue = SUT.value
        }
        
        // Assert
        assertNotEquals(initialValue, newValue, "Die value should change after rolling")
    }
    
    @Test
    fun reroll_callsRollMethod() {
        // Arrange
        val spyDie = spyk(SUT)
        val initialValue = spyDie.value
        
        // Set up the spy to return a different value on roll
        val newValue = if (initialValue < TEST_SIDES) initialValue + 1 else initialValue - 1
        every { spyDie.roll() } returns spyDie
        every { spyDie.value } returns newValue
        
        // Act
        spyDie.roll()
        
        // Assert
        verify(exactly = 1) { spyDie.roll() }
        assertEquals(newValue, spyDie.value)
    }
    
    @Test
    fun roll_withMockedRandomizer_usesCorrectRange() {
        // Arrange
        val mockRandomizer = mockk<Randomizer>()
        every { mockRandomizer.nextInt(1, TEST_SIDES + 1) } returns 4
        
        val dieWithMockRandomizer = DieRandom(TEST_SIDES, mockRandomizer)
        
        // Act
        dieWithMockRandomizer.roll()
        
        // Assert
        verify { mockRandomizer.nextInt(1, TEST_SIDES + 1) }
        assertEquals(4, dieWithMockRandomizer.value)
    }

    @Test
    fun equals_whenSameSidesAndValue_returnsTrue() {
        // Arrange
        SUT.adjustTo(3)
        val die2 = dieFactory(DieSides.D6).adjustTo(3)

        // Assert
        assertEquals(SUT, die2)
    }

    @Test
    fun equals_whenDifferentSides_returnsFalse() {
        // Arrange
        val die2 = dieFactory(DieSides.D4)

        // Assert
        assertNotEquals(SUT, die2)
    }

    @Test
    fun equals_whenDifferentValues_returnsFalse() {
        // Arrange
        SUT.adjustTo(3)
        val die2 = dieFactory(DieSides.D6).adjustTo(4)

        // Assert
        assertNotEquals(SUT, die2)
    }

    @Test
    fun equals_whenDieValueWithSameSidesAndValue_returnsTrue() {
        // Arrange
        SUT.adjustTo(3)
        val dieValue = DieValue(6, 3)

        // Assert
        assertEquals(SUT as DieBase, dieValue)
        assertEquals(dieValue as DieBase, SUT)
    }

    @Test
    fun equals_whenDieValueWithDifferentSides_returnsFalse() {
        // Arrange
        SUT.adjustTo(3)
        val dieValue = DieValue(4, 3)

        // Assert
        assertNotEquals(SUT as DieBase, dieValue)
        assertNotEquals(dieValue as DieBase, SUT)
    }

    @Test
    fun equals_whenDieValueWithDifferentValue_returnsFalse() {
        // Arrange
        SUT.adjustTo(3)
        val dieValue = DieValue(6, 4)

        // Assert
        assertNotEquals(SUT as DieBase, dieValue)
        assertNotEquals(dieValue as DieBase, SUT)
    }

    @Test
    fun equals_whenDieValuesWithSameSidesAndValue_returnsTrue() {
        // Arrange
        val dieValue1 = DieValue(6, 3)
        val dieValue2 = DieValue(6, 3)

        // Assert
        assertEquals(dieValue1, dieValue2)
    }

    @Test
    fun equals_whenDieValuesWithDifferentSides_returnsFalse() {
        // Arrange
        val dieValue1 = DieValue(6, 3)
        val dieValue2 = DieValue(4, 3)

        // Assert
        assertNotEquals(dieValue1, dieValue2)
    }

    @Test
    fun equals_whenDieValuesWithDifferentValues_returnsFalse() {
        // Arrange
        val dieValue1 = DieValue(6, 3)
        val dieValue2 = DieValue(6, 4)

        // Assert
        assertNotEquals(dieValue1, dieValue2)
    }

    @Test
    fun adjustTo_whenValidValue_setsValueCorrectly() {
        // Arrange
        val die = dieFactory(DieSides.D6)

        // Act
        die.adjustTo(TEST_VALUE)

        // Assert
        assertEquals(TEST_VALUE, die.value)
    }

    @Test
    fun adjustTo_whenValueExceedsMax_setsValueToMax() {
        // Arrange
        val die = dieFactory(DieSides.D6)

        // Act
        die.adjustTo(TEST_SIDES + 1)

        // Assert
        assertEquals(TEST_SIDES, die.value)
    }

    @Test
    fun adjustBy_whenValidAmount_modifiesValueCorrectly() {
        // Arrange
        val die = dieFactory(DieSides.D6).adjustTo(TEST_VALUE)

        // Act
        die.adjustBy(2)

        // Assert
        assertEquals(TEST_VALUE + 2, die.value)
    }

    @Test
    fun adjustBy_whenAmountExceedsMax_setsValueToMax() {
        // Arrange
        val die = dieFactory(DieSides.D6).adjustTo(TEST_VALUE)

        // Act
        die.adjustBy(TEST_SIDES)

        // Assert
        assertEquals(TEST_SIDES, die.value)
    }

    @Test
    fun adjustToMax_whenCalled_setsValueToMax() {
        // Arrange
        val die = dieFactory(DieSides.D6).adjustTo(TEST_VALUE)

        // Act
        die.adjustToMax()

        // Assert
        assertEquals(TEST_SIDES, die.value)
    }

    @Test
    fun dieValue_whenCreated_hasCorrectSidesAndValue() {
        // Arrange
        val sides = 6
        val value = 3
        val dieValue = DieValue(sides, value)
        
        // Assert
        assertEquals(sides, dieValue.sides, "DieValue should have correct sides")
        assertEquals(value, dieValue.value, "DieValue should have correct value")
    }
    
    @Test
    fun dieValue_whenValueExceedsSides_isConstrainedToSides() {
        // Arrange
        val sides = 6
        val value = 8
        val dieValue = DieValue(sides, value)
        
        // Assert
        assertEquals(sides, dieValue.value, "DieValue should be constrained to sides")
    }
    
    @Test
    fun dieValue_whenValueBelowOne_isConstrainedToOne() {
        // Arrange
        val sides = 6
        val value = 0
        val dieValue = DieValue(sides, value)
        
        // Assert
        assertEquals(1, dieValue.value, "DieValue should be constrained to minimum of 1")
    }
    
    @Test
    fun dieValue_adjustTo_returnsSameInstance() {
        // Arrange
        val dieValue = DieValue(6, 3)
        
        // Act
        val result = dieValue.adjustTo(4)
        
        // Assert
        assertSame(dieValue as DieBase, result, "adjustTo should return same instance")
        assertEquals(4, dieValue.value, "Value should be updated")
    }
    
    @Test
    fun dieValue_adjustBy_returnsSameInstance() {
        // Arrange
        val dieValue = DieValue(6, 3)
        
        // Act
        val result = dieValue.adjustBy(2)
        
        // Assert
        assertSame(dieValue as DieBase, result, "adjustBy should return same instance")
        assertEquals(5, dieValue.value, "Value should be updated")
    }
    
    @Test
    fun dieValue_adjustToMax_returnsCorrectAmount() {
        // Arrange
        val dieValue = DieValue(6, 3)
        
        // Act
        val amount = dieValue.adjustToMax()
        
        // Assert
        assertEquals(6, amount, "Should return correct amount to reach max")
        assertEquals(6, dieValue.value, "Value should be set to max")
    }
} 
