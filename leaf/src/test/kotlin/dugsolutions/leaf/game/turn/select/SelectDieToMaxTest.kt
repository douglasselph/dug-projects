package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.factory.DieFactory
import dugsolutions.leaf.di.factory.DieFactoryRandom
import dugsolutions.leaf.tool.Randomizer
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectDieToMaxTest {

    private lateinit var mockBloomCard: GameCard
    private lateinit var mockRootCard: GameCard
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die

    private lateinit var SUT: SelectDieToMax

    @BeforeEach
    fun setup() {
        // Initialize random components
        randomizer = Randomizer.create()
        dieFactory = DieFactory(randomizer)

        // Create dice
        d4 = dieFactory(DieSides.D4)
        d6 = dieFactory(DieSides.D6)
        d8 = dieFactory(DieSides.D8)

        // Create mock cards
        mockBloomCard = mockk()
        mockRootCard = mockk()

        SUT = SelectDieToMax()
    }

    @Test
    fun invoke_whenEmptyDiceList_returnsNull() {
        // Act
        val result = SUT(Dice())

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenSingleDieNotAtMax_returnsThatDie() {
        // Arrange
        d4.adjustTo(2)
        val dice = Dice(listOf(d4))

        // Act
        val result = SUT(dice)

        // Assert
        assertEquals(d4, result)
    }

    @Test
    fun invoke_whenSingleDieAtMax_returnsThatDie() {
        // Arrange
        d4.adjustTo(4)
        val dice = Dice(listOf(d4))

        // Act
        val result = SUT(dice)

        // Assert
        assertEquals(d4, result)
    }

    @Test
    fun invoke_whenMultipleDiceNotAtMax_returnsFirstNotAtMaxWithBestDifference() {
        // Arrange
        d4.adjustTo(4)
        d6.adjustTo(3)
        d8.adjustTo(5)
        val dice = Dice(listOf(d4, d6, d8))

        // Act
        val result = SUT(dice)

        // Assert
        assertEquals(d6, result)
    }

    @Test
    fun invoke_whenMultipleDiceNotAtMax_returnsBestDifference() {
        // Arrange
        d4.adjustTo(1)
        d6.adjustTo(5)
        d8.adjustTo(6)
        val dice = Dice(listOf(d4, d6, d8))

        // Act
        val result = SUT(dice)

        // Assert
        assertEquals(d4, result)
    }

    @Test
    fun invoke_whenDiceInDifferentOrder_returnsFirstNotAtMax() {
        // Arrange
        d4.adjustTo(1)
        d8.adjustTo(1)
        d6.adjustTo(1)
        val dice = Dice(listOf(d4, d8, d6))

        // Act
        val result = SUT(dice)

        // Assert
        assertEquals(d8, result)
    }
} 
