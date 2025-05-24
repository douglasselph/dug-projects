package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.tool.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectDieToAdjustTest {

    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die

    private lateinit var SUT: SelectDieToAdjust

    @BeforeEach
    fun setup() {
        SUT = SelectDieToAdjust()
        
        // Initialize random components
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)

        // Create test dice
        d4 = dieFactory(DieSides.D4)
        d6 = dieFactory(DieSides.D6)
        d8 = dieFactory(DieSides.D8)
    }

    @Test
    fun invoke_whenEmptyDiceList_returnsNull() {
        // Act
        val result = SUT(Dice(), 2)

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenAllDiceTooLarge_ReturnsBestDieThatCanIncreaseTheMost() {
        // Arrange
        d4.adjustTo(3)
        d6.adjustTo(6)
        d8.adjustTo(5)
        val dice = Dice(listOf(d4, d6, d8))

        // Act
        val result = SUT(dice, 2)

        // Assert
        assertEquals(d8, result)
    }

    @Test
    fun invoke_byPositiveAmount_returnsFirstDieThatCanHandleIt() {
        // Arrange
        d4.adjustTo(2)
        d6.adjustTo(3)
        d8.adjustTo(5)
        val dice = Dice(listOf(d4, d6, d8))

        // Act
        val result = SUT(dice, 2)

        // Assert
        assertEquals(d4, result)
    }

    @Test
    fun invoke_byPositiveAmount_returnsFirstDieThatCanHandleIt2() {
        // Arrange
        d4.adjustTo(4)
        d6.adjustTo(5)
        d8.adjustTo(7)
        val dice = Dice(listOf(d4, d6, d8))

        // Act
        val result = SUT(dice, 2)

        // Assert
        assertEquals(d6, result)
    }

    @Test
    fun invoke_whenAllDiceAtMax_returnsNull() {
        // Arrange
        d4.adjustTo(4)
        d6.adjustTo(6)
        d8.adjustTo(8)
        val dice = Dice(listOf(d4, d6, d8))

        // Act
        val result = SUT(dice, 2)

        // Assert
        assertEquals(d4, result)
    }

    @Test
    fun invoke_whenAdjustmentIsNegative_returnsFirstLargeEnoughToHandleIt() {
        // Arrange
        d4.adjustTo(3)
        d6.adjustTo(5)
        d8.adjustTo(7)
        val dice = Dice(listOf(d4, d6, d8))

        // Act
        val result = SUT(dice, -2)

        // Assert
        assertEquals(d4, result)
    }

    @Test
    fun invoke_whenAdjustmentIsNegative_returnsFirstLargeEnoughToHandleIt2() {
        // Arrange
        d4.adjustTo(1)
        d6.adjustTo(5)
        d8.adjustTo(7)
        val dice = Dice(listOf(d4, d6, d8))

        // Act
        val result = SUT(dice, -2)

        // Assert
        assertEquals(d6, result)
    }

    @Test
    fun invoke_whenAdjustmentIsNegative_returnsFirstLargeEnoughToHandleIt3() {
        // Arrange
        d4.adjustTo(1)
        d6.adjustTo(2)
        d8.adjustTo(7)
        val dice = Dice(listOf(d4, d6, d8))

        // Act
        val result = SUT(dice, -2)

        // Assert
        assertEquals(d8, result)
    }
} 
