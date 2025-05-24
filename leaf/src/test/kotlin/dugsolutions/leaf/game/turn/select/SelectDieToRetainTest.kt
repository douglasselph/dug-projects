package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.tool.Randomizer
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectDieToRetainTest {

    private lateinit var mockRandomizer: Randomizer
    private lateinit var sampleDie: SampleDie
    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die

    private lateinit var SUT: SelectDieToRetain

    @BeforeEach
    fun setup() {
        // Initialize test components
        mockRandomizer = mockk(relaxed = true)
        sampleDie = SampleDie()
        
        // Create test dice with fixed values
        d4 = sampleDie.d4.adjustTo(3)  // D4 showing 3
        d6 = sampleDie.d6.adjustTo(4)  // D6 showing 4
        d8 = sampleDie.d8.adjustTo(7)  // D8 showing 7

        // Initialize component under test
        SUT = SelectDieToRetain(mockRandomizer)
    }

    @Test
    fun invoke_whenRandomizerReturnsTrue_returnsNull() {
        // Arrange
        every { mockRandomizer.nextBoolean() } returns true

        // Act
        val result = SUT(Dice(listOf(d4, d6, d8)))

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenRandomizerReturnsFalse_returnsRandomDie() {
        // Arrange
        every { mockRandomizer.nextBoolean() } returns false
        every { mockRandomizer.randomOrNull(any<List<Die>>()) } returns d6

        // Act
        val result = SUT(Dice(listOf(d4, d6, d8)))

        // Assert
        assertEquals(d6, result)
    }

    @Test
    fun invoke_whenDiceListIsEmpty_returnsNull() {
        // Arrange
        every { mockRandomizer.nextBoolean() } returns false
        every { mockRandomizer.randomOrNull(any<List<Die>>()) } returns null

        // Act
        val result = SUT(Dice())

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenRandomizerReturnsFalseAndNoDice_returnsNull() {
        // Arrange
        every { mockRandomizer.nextBoolean() } returns false
        every { mockRandomizer.randomOrNull(any<List<Die>>()) } returns null

        // Act
        val result = SUT(Dice(listOf(d4, d6, d8)))

        // Assert
        assertNull(result)
    }
} 
