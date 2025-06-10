package dugsolutions.leaf.main.gather

import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.main.domain.DieInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class GatherADieInfoTest {

    companion object {
        private const val D4_VALUE = "D4=1"
        private const val D6_VALUE = "D6=3"
        private const val D8_VALUE = "D8=5"
        private const val D20_VALUE = "D20=15"
        private const val D4_COUNT = "2D4"
        private const val D6_COUNT = "2D6"
        private const val D8_COUNT = "1D8"
        private const val D20_COUNT = "1D20"
    }

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
        val expectedValues = listOf(
            DieInfo(index = 0, value = D4_VALUE, backingDie = dice.dice[0]),
            DieInfo(index = 1, value = D6_VALUE, backingDie = dice.dice[1]),
            DieInfo(index = 2, value = D8_VALUE, backingDie = dice.dice[2])
        )
        assertEquals(expectedValues, result.values)
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
        val expectedValues = listOf(
            DieInfo(value = D4_COUNT),
            DieInfo(value = D6_COUNT),
            DieInfo(value = D8_COUNT)
        )
        assertEquals(expectedValues, result.values)
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
        val expectedWithValues = listOf(DieInfo(index = 0, value = D20_VALUE, backingDie = dice.dice[0]))
        val expectedWithoutValues = listOf(DieInfo(value = D20_COUNT))
        assertEquals(expectedWithValues, resultWithValues.values)
        assertEquals(expectedWithoutValues, resultWithoutValues.values)
    }
} 
