package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.factory.DieFactory
import dugsolutions.leaf.di.factory.DieFactoryRandom
import dugsolutions.leaf.tool.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SelectDieToRerollTest {

    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die

    private lateinit var SUT: SelectDieToReroll

    @BeforeEach
    fun setup() {
        // Initialize random components
        randomizer = Randomizer.create()
        dieFactory = DieFactory(randomizer)

        // Create test dice
        d4 = dieFactory(DieSides.D4)
        d6 = dieFactory(DieSides.D6)
        d8 = dieFactory(DieSides.D8)

        SUT = SelectDieToReroll()
    }

    @Test
    fun invoke_returnsDieWithGreatestDifference() {
        // Arrange
        d4.adjustTo(1)
        d6.adjustTo(1)
        d8.adjustTo(1)
        // Act
        val result = SUT(Dice(listOf(d4, d6, d8)))

        // Assert
        assertEquals(d8, result)
    }

    @Test
    fun invoke_returnsDieWithGreatestDifference2() {
        // Arrange
        d4.adjustTo(1)
        d6.adjustTo(1)
        d8.adjustTo(7)
        // Act
        val result = SUT(Dice(listOf(d4, d6, d8)))

        // Assert
        assertEquals(d6, result)
    }

    @Test
    fun invoke_returnsDieWithGreatestDifference3() {
        // Arrange
        d4.adjustTo(1)
        d6.adjustTo(4)
        d8.adjustTo(7)
        // Act
        val result = SUT(Dice(listOf(d4, d6, d8)))

        // Assert
        assertEquals(d4, result)
    }
} 
