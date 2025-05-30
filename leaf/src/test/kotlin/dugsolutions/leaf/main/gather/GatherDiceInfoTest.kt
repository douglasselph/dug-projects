package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.SampleDie
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class GatherDiceInfoTest {

    private lateinit var sampleDie: SampleDie
    private lateinit var SUT: GatherDiceInfo

    @BeforeEach
    fun setup() {
        sampleDie = SampleDie()
        SUT = GatherDiceInfo()
    }

    @Test
    fun invoke_whenValuesTrue_returnsDiceWithValues() {
        // Arrange
        val dice = Dice(listOf(
            sampleDie.d4.adjustTo(1),
            sampleDie.d6.adjustTo(3),
            sampleDie.d8.adjustTo(5)
        ))

        // Act
        val result = SUT(dice, values = true)

        // Assert
        assertEquals(listOf("D4=1", "D6=3", "D8=5"), result.values)
    }

    @Test
    fun invoke_whenValuesFalse_returnsDiceCounts() {
        // Arrange
        val dice = Dice(listOf(
            sampleDie.d4.adjustTo(1),
            sampleDie.d4.adjustTo(2),
            sampleDie.d6.adjustTo(3),
            sampleDie.d6.adjustTo(4),
            sampleDie.d8.adjustTo(5)
        ))

        // Act
        val result = SUT(dice, values = false)

        // Assert
        assertEquals(listOf("2D4", "2D6", "1D8"), result.values)
    }

    @Test
    fun invoke_whenEmptyDice_returnsEmptyList() {
        // Arrange
        val dice = Dice(emptyList())

        // Act
        val resultWithValues = SUT(dice, values = true)
        val resultWithoutValues = SUT(dice, values = false)

        // Assert
        assertTrue(resultWithValues.values.isEmpty())
        assertTrue(resultWithoutValues.values.isEmpty())
    }

    @Test
    fun invoke_whenSingleDie_returnsCorrectFormat() {
        // Arrange
        val dice = Dice(listOf(sampleDie.d20.adjustTo(15)))

        // Act
        val resultWithValues = SUT(dice, values = true)
        val resultWithoutValues = SUT(dice, values = false)

        // Assert
        assertEquals(listOf("D20=15"), resultWithValues.values)
        assertEquals(listOf("1D20"), resultWithoutValues.values)
    }
} 
