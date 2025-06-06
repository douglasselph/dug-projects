package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.game.acquire.domain.FakeCombination
import dugsolutions.leaf.game.acquire.domain.FakeCombination.sampleDie
import dugsolutions.leaf.game.turn.select.SelectPossibleDice
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PossibleDiceTest {

    private val selectPossibleDice = mockk<SelectPossibleDice>(relaxed = true)
    private val dieCost = mockk<DieCost>(relaxed = true)

    private val SUT = PossibleDice(selectPossibleDice, dieCost)

    @Test
    fun invoke_whenNoCombinations_returnsEmptyList() {
        // Arrange
        val combinations = Combinations(emptyList())

        // Act
        val result = SUT(combinations)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_whenNoMarketDice_returnsEmptyList() {
        // Arrange
        val combination = FakeCombination.combinationD6
        val combinations = Combinations(listOf(combination))
        every { selectPossibleDice() } returns emptyList()

        // Act
        val result = SUT(combinations)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_whenDiceAvailable_returnsBestDieForEachCombination() {
        // Arrange
        val combination1 = FakeCombination.combinationD6
        val combination2 = FakeCombination.combinationD4D6
        val combinations = Combinations(listOf(combination1, combination2))

        val marketDice = listOf(sampleDie.d4, sampleDie.d6, sampleDie.d8, sampleDie.d10)
        every { selectPossibleDice() } returns marketDice
        every { dieCost(sampleDie.d4) } returns 1
        every { dieCost(sampleDie.d6) } returns 2
        every { dieCost(sampleDie.d8) } returns 3
        every { dieCost(sampleDie.d10) } returns 4

        // Act
        val result = SUT(combinations)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it.die == sampleDie.d6 })
        assertTrue(result.any { it.die == sampleDie.d8 })
    }

    @Test
    fun invoke_whenNoAffordableDice_returnsEmptyList() {
        // Arrange
        val combination = FakeCombination.combinationD4D6D8
        val combinations = Combinations(listOf(combination))

        val marketDice = listOf(sampleDie.d8, sampleDie.d10)
        every { selectPossibleDice() } returns marketDice
        every { dieCost(sampleDie.d8) } returns 10
        every { dieCost(sampleDie.d10) } returns 15

        // Act
        val result = SUT(combinations)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_whenMultipleDiceWithSameCost_returnsHighestSidedDie() {
        // Arrange
        val combination = FakeCombination.combinationD8
        val combinations = Combinations(listOf(combination))

        val marketDice = listOf(sampleDie.d6, sampleDie.d8)
        every { selectPossibleDice() } returns marketDice
        every { dieCost(sampleDie.d6) } returns 2
        every { dieCost(sampleDie.d8) } returns 2

        // Act
        val result = SUT(combinations)

        // Assert
        assertEquals(1, result.size)
        assertEquals(sampleDie.d8, result[0].die)
    }

    @Test
    fun invoke_whenCombinationHasAdditionalTotal_usesTotalInCalculation() {
        // Arrange
        val combination = FakeCombination.combinationD6Plus5
        val combinations = Combinations(listOf(combination))

        val marketDice = listOf(sampleDie.d8, sampleDie.d10)
        every { selectPossibleDice() } returns marketDice
        every { dieCost(sampleDie.d8) } returns 7
        every { dieCost(sampleDie.d10) } returns 12

        // Act
        val result = SUT(combinations)

        // Assert
        assertEquals(1, result.size)
        assertEquals(sampleDie.d8, result[0].die)
    }
} 
