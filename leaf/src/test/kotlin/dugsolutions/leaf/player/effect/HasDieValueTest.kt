package dugsolutions.leaf.player.effect

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.tool.Randomizer
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasDieValueTest {

    private lateinit var hasDieValue: HasDieValue
    private lateinit var sampleHandCard: HandItem.Card
    private lateinit var sampleHandDice: HandItem.Dice
    private lateinit var sampleDie: Die
    private lateinit var mockGameCard: GameCard
    private lateinit var randomizer: Randomizer
    private lateinit var dieFactory: DieFactory

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)
        mockGameCard = mockk()

        hasDieValue = HasDieValue()

        sampleHandCard = HandItem.Card(mockGameCard)
        sampleDie = dieFactory(DieSides.D8)
        sampleHandDice = HandItem.Dice(sampleDie)
    }

    @Test
    fun invoke_whenEmptyList_returnsFalse() {
        // Act
        val result = hasDieValue(emptyList(), 6)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenOnlyCards_returnsFalse() {
        // Act
        val result = hasDieValue(listOf(sampleHandCard), 6)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenDiceWithMatchingValue_returnsTrue() {
        // Act
        sampleDie.adjustTo(6)
        val result = hasDieValue(listOf(sampleHandDice), 6)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenDiceWithDifferentValue_returnsFalse() {
        // Arrange
        sampleDie.adjustTo(3)

        // Act
        val result = hasDieValue(listOf(sampleHandDice), 4)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenMixedItemsWithMatchingDice_returnsTrue() {
        // Act
        sampleDie.adjustTo(6)
        val result = hasDieValue(listOf(sampleHandCard, sampleHandDice), 6)

        // Assert
        assertTrue(result)
    }
} 