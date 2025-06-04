package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.game.acquire.domain.FakeCombination
import dugsolutions.leaf.game.acquire.domain.FakeCombination.sampleDie
import dugsolutions.leaf.game.turn.select.SelectPossibleDice
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PossibleBestDiceTest {

    companion object {
        val d4 = sampleDie.d4
        val d6 = sampleDie.d6
        val d8 = sampleDie.d8
        val d10 = sampleDie.d10
    }

    private val selectPossibleDice = mockk<SelectPossibleDice>(relaxed = true)
    private val dieCost = mockk<DieCost>(relaxed = true)

    private val SUT = PossibleBestDice(selectPossibleDice, dieCost)

    @BeforeEach
    fun setup() {
        every { dieCost(d4) } returns 4
        every { dieCost(d6) } returns 6
        every { dieCost(d8) } returns 8
        every { dieCost(d10) } returns 10
    }

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
        val combination1 = Combination(
            DieValues(
                Dice(
                    listOf(sampleDie.d6.adjustTo(4))
                ).copy
            ),
            addToTotal = 0
        )
        val combination2 = Combination(
            DieValues(
                Dice(
                    listOf(sampleDie.d4.adjustTo(3), sampleDie.d6.adjustTo(5))
                ).copy
            ),
            addToTotal = 0
        )
        val combinations = Combinations(listOf(combination1, combination2))

        val marketDice = listOf(d4, d6, d8, d10)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        assertEquals(2, result.size)
        assertEquals(4, result[0].die.sides)
        assertEquals(8, result[1].die.sides)
    }

    @Test
    fun invoke_whenNoAffordableDice_returnsEmptyList() {
        // Arrange
        val combination = Combination(
            DieValues(
                Dice(
                    listOf(sampleDie.d6.adjustTo(4))
                ).copy
            ),
            addToTotal = 0
        )
        val combinations = Combinations(listOf(combination))

        val marketDice = listOf(d8, d10)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_whenCombinationHasAdditionalTotal_usesTotalInCalculation() {
        // Arrange
        val combination = Combination(
            DieValues(
                Dice(
                    listOf(sampleDie.d4.adjustTo(2), sampleDie.d6.adjustTo(6))
                ).copy
            ),
            addToTotal = 2
        )
        val combinations = Combinations(listOf(combination))

        val marketDice = listOf(d4, d6, d8, d10)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        assertEquals(1, result.size)
        assertEquals(d10, result[0].die)
    }
} 
