package dugsolutions.leaf.player.effect

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.Randomizer
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HasDieValueTest {

    private lateinit var sampleHandCard: HandItem.aCard
    private lateinit var sampleHandADie: HandItem.aDie
    private lateinit var sampleDie: Die
    private lateinit var mockGameCard: GameCard
    private lateinit var randomizer: Randomizer
    private lateinit var dieFactory: DieFactory

    private lateinit var SUT: HasDieValue

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        dieFactory = DieFactory(randomizer)
        mockGameCard = mockk()

        SUT = HasDieValue()

        sampleHandCard = HandItem.aCard(mockGameCard)
        sampleDie = dieFactory(DieSides.D8)
        sampleHandADie = HandItem.aDie(sampleDie)
    }

    @Test
    fun invoke_whenEmptyList_returnsFalse() {
        // Arrange
        // Act
        val result = SUT(emptyList(), 6)

        // Assert
        assertEquals(null, result)
    }

    @Test
    fun invoke_whenOnlyCards_returnsFalse() {
        // Act
        val result = SUT(listOf(sampleHandCard), 6)

        // Assert
        assertEquals(null, result)
    }

    @Test
    fun invoke_whenDiceWithMatchingValue_returnsTrue() {
        // Act
        sampleDie.adjustTo(6)

        val result = SUT(listOf(sampleHandADie), 6)

        // Assert
        assertEquals(sampleDie, result)
    }

    @Test
    fun invoke_whenDiceWithDifferentValue_returnsFalse() {
        // Arrange
        sampleDie.adjustTo(3)

        // Act
        val result = SUT(listOf(sampleHandADie), 4)

        // Assert
        assertEquals(null, result)
    }

    @Test
    fun invoke_whenMixedItemsWithMatchingDice_returnsTrue() {
        // Act
        sampleDie.adjustTo(6)
        val result = SUT(listOf(sampleHandCard, sampleHandADie), 6)

        // Assert
        assertEquals(sampleDie, result)
    }
} 
