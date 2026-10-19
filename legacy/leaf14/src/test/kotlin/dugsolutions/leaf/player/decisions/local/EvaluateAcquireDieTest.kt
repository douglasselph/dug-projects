package dugsolutions.leaf.player.local

import dugsolutions.leaf.common.domain.acquire.ChoiceDie
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireDie
import dugsolutions.leaf.random.FakeUsingDice
import dugsolutions.leaf.random.die.SampleDie
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EvaluateAcquireDieTest {

    private val sampleDie = SampleDie()

    private val SUT = EvaluateAcquireDie()

    @Test
    fun invoke_whenMultipleDice_returnsHighestSidedDie() {
        // Arrange
        val choice1 = ChoiceDie(sampleDie.d6, FakeUsingDice.d6)
        val choice2 = ChoiceDie(sampleDie.d8, FakeUsingDice.d8)
        val choice3 = ChoiceDie(sampleDie.d10, FakeUsingDice.d10)

        // Act
        val result = SUT(listOf(choice1, choice2, choice3))

        // Assert
        assertEquals(choice3, result)
    }

    @Test
    fun invoke_whenSameSidedDice_returnsFewestDiceValues() {
        // Arrange
        val choice1 = ChoiceDie(sampleDie.d8, FakeUsingDice.d4D6)
        val choice2 = ChoiceDie(sampleDie.d8, FakeUsingDice.d10)
        val choice3 = ChoiceDie(sampleDie.d8, FakeUsingDice.d4D6D8)

        // Act
        val result = SUT(listOf(choice1, choice2, choice3))

        // Assert
        assertEquals(choice2, result)
    }

    @Test
    fun invoke_whenEmptyList_returnsNull() {
        // Act
        val result = SUT(emptyList())

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenSingleChoice_returnsThatChoice() {
        // Arrange
        val choice = ChoiceDie(sampleDie.d6, FakeUsingDice.d6)

        // Act
        val result = SUT(listOf(choice))

        // Assert
        assertEquals(choice, result)
    }

    @Test
    fun invoke_whenSameSidesAndSameDiceCount_returnsFirstChoice() {
        // Arrange
        val choice1 = ChoiceDie(sampleDie.d8, FakeUsingDice.d10)
        val choice2 = ChoiceDie(sampleDie.d8, FakeUsingDice.d10)

        // Act
        val result = SUT(listOf(choice1, choice2))

        // Assert
        assertEquals(choice1, result)
    }
} 
