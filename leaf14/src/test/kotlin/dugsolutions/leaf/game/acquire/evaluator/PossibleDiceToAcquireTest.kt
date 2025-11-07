package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.grove.SelectPossibleDice
import dugsolutions.leaf.random.die.DieCost
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValues
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PossibleDiceToAcquireTest {

    companion object {
        private val sampleDie = SampleDie()
        private val d4 = sampleDie.d4
        private val d6 = sampleDie.d6
        private val d8 = sampleDie.d8
        private val d10 = sampleDie.d10
    }

    private val selectPossibleDice = mockk<SelectPossibleDice>(relaxed = true)
    private val dieCost = mockk<DieCost>(relaxed = true)

    private val SUT = PossibleDiceToAcquire(selectPossibleDice, dieCost)

    @BeforeEach
    fun setup() {
        every { dieCost.invoke(any<Die>()) } answers { firstArg<Die>().sides }
    }

    @Test
    fun invoke_whenEmptyList_returnsEmptyList() {
        // Arrange
        val combinations: List<DieValues> = emptyList()

        // Act
        val result = SUT(combinations)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_whenNoMarketDice_returnsEmptyList() {
        // Arrange
        val combination = DieValues.from(listOf(d6.adjustTo(4)))
        val combinations = listOf(combination)
        every { selectPossibleDice() } returns emptyList()

        // Act
        val result = SUT(combinations)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_whenDiceAvailable_returnsBestDieForEachCombination() {
        // Arrange
        val combination1 = DieValues.from(listOf(d6.adjustTo(4)))
        val combination2 = DieValues.from(listOf(d4.adjustTo(3), d6.adjustTo(5)))
        val combinations = listOf(combination1, combination2)

        val marketDice = listOf(d4, d6, d8, d10)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        assertEquals(2, result.size)
        assertEquals(4, result[0].die.sides) // d4 for combination1 (total=4)
        assertEquals(8, result[1].die.sides) // d8 for combination2 (total=8)
    }

    @Test
    fun invoke_whenNoAffordableDice_returnsEmptyList() {
        // Arrange
        val combination = DieValues.from(listOf(d6.adjustTo(4)))
        val combinations = listOf(combination)

        val marketDice = listOf(d8, d10)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_whenMultipleDiceInCombination_usesTotalInCalculation() {
        // Arrange
        val combination = DieValues.from(listOf(d4.adjustTo(2), d6.adjustTo(6)))
        val combinations = listOf(combination)

        val marketDice = listOf(d4, d6, d8, d10)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        assertEquals(1, result.size)
        assertEquals(d10, result[0].die) // Total = 8, can afford d10 (cost=10), but wait...
        // Actually, total = 2 + 6 = 8, so can afford d8 (cost=8) or d10 (cost=10)
        // maxByOrNull will pick d10, but wait - it filters by dieCost(die) <= combination.total
        // So only d8 can be afforded (cost=8 <= 8), d10 (cost=10 > 8) cannot
        // So it should return d8
        assertEquals(d8, result[0].die)
    }

    @Test
    fun invoke_whenMultipleCombinationsCanBuySameDie_returnsOnlyBestOne() {
        // Arrange
        val combination1 = DieValues.from(listOf(d4.adjustTo(2), d6.adjustTo(6))) // total = 8
        val combination2 = DieValues.from(listOf(d8.adjustTo(8))) // total = 8
        val combinations = listOf(combination1, combination2)

        val marketDice = listOf(d4, d6, d8, d10)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        // Both combinations can afford d8 (cost=8 <= 8)
        // addIfBetter will keep the one with lower total (or same total, but first one)
        // Since both have total=8, the first one should be kept
        assertEquals(1, result.size)
        assertEquals(d8, result[0].die)
        assertEquals(combination1, result[0].usingDice) // First combination is kept
    }

    @Test
    fun invoke_whenMultipleCombinationsDifferentSides_returnsBoth() {
        // Arrange
        val combination1 = DieValues.from(listOf(d4.adjustTo(4))) // total = 4, can afford d4
        val combination2 = DieValues.from(listOf(d6.adjustTo(6))) // total = 6, can afford d6
        val combinations: List<DieValues> = listOf(combination1, combination2)

        val marketDice = listOf(d4, d6, d8, d10)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        assertEquals(2, result.size)
        // Results are sorted by die.sides, then by usingDice.total
        assertEquals(4, result[0].die.sides)
        assertEquals(6, result[1].die.sides)
    }

    @Test
    fun invoke_whenSameSidedDiceWithBetterTotal_keepsBetterOne() {
        // Arrange
        val combination1 = DieValues.from(listOf(d4.adjustTo(4))) // total = 4
        val combination2 = DieValues.from(listOf(d4.adjustTo(2))) // total = 2
        val combinations: List<DieValues> = listOf(combination1, combination2)

        val marketDice = listOf(d4)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        // Both can afford d4 (cost=4)
        // addIfBetter keeps the one with lower total (better efficiency)
        assertEquals(1, result.size)
        assertEquals(d4, result[0].die)
        assertEquals(combination2, result[0].usingDice) // Lower total is better
    }

    @Test
    fun invoke_whenResultsSortedByDieSidesThenTotal() {
        // Arrange
        val combination1 = DieValues.from(listOf(d6.adjustTo(6))) // total = 6
        val combination2 = DieValues.from(listOf(d4.adjustTo(4))) // total = 4
        val combinations = listOf(combination1, combination2)

        val marketDice = listOf(d4, d6, d8)
        every { selectPossibleDice() } returns marketDice

        // Act
        val result = SUT(combinations)

        // Assert
        assertEquals(2, result.size)
        // Sorted by die.sides first, then by usingDice.total
        assertEquals(4, result[0].die.sides)
        assertEquals(6, result[1].die.sides)
        assertEquals(4, result[0].usingDice.total)
        assertEquals(6, result[1].usingDice.total)
    }
}
