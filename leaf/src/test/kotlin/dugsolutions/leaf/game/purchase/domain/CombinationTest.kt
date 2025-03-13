package dugsolutions.leaf.game.purchase.domain

import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CombinationTest {

    @Test
    fun totalValue_withEmptyCombination_returnsZero() {
        // Arrange
        val values = DieValues(emptyList())
        val combination = Combination(
            values = values,
            addToTotal = 0
        )

        // Act
        val result = combination.totalValue

        // Assert
        assertEquals(0, result, "Empty combination should have total value of 0")
    }

    @Test
    fun totalValue_withOnlyDice_returnsSum() {
        // Arrange
        val dieValues = listOf(
            DieValue(6, 3),  // d6 with value 3
            DieValue(8, 5)   // d8 with value 5
        )
        val values = DieValues(dieValues)
        val combination = Combination(
            values = values,
            addToTotal = 0
        )

        // Act
        val result = combination.totalValue

        // Assert
        assertEquals(8, result, "Total value should be sum of die values (3 + 5 = 8)")
    }

    @Test
    fun totalValue_withOnlyAddToTotal_returnsAddedValue() {
        // Arrange
        val addToTotal = 5
        val values = DieValues(emptyList())
        val combination = Combination(
            values = values,
            addToTotal = addToTotal
        )

        // Act
        val result = combination.totalValue

        // Assert
        assertEquals(addToTotal, result, "Total value should equal addToTotal when no dice present")
    }

    @Test
    fun totalValue_withDiceAndAddToTotal_returnsCombinedSum() {
        // Arrange
        val dieValues = listOf(
            DieValue(4, 2),  // d4 with value 2
            DieValue(6, 4)   // d6 with value 4
        )
        val values = DieValues(dieValues)
        val addToTotal = 3
        val combination = Combination(
            values = values,
            addToTotal = addToTotal
        )

        // Act
        val result = combination.totalValue

        // Assert
        assertEquals(9, result, "Total value should be sum of die values plus addToTotal (2 + 4 + 3 = 9)")
    }

    @Test
    fun totalValue_withMultipleDice_returnsTotalSum() {
        // Arrange
        val dieValues = listOf(
            DieValue(4, 2),   // d4 with value 2
            DieValue(6, 3),   // d6 with value 3
            DieValue(8, 5),   // d8 with value 5
            DieValue(10, 7)   // d10 with value 7
        )
        val values = DieValues(dieValues)
        val addToTotal = 5
        val combination = Combination(
            values = values,
            addToTotal = addToTotal
        )

        // Act
        val result = combination.totalValue

        // Assert
        assertEquals(22, result, "Total value should be sum of all die values plus addToTotal (2 + 3 + 5 + 7 + 5 = 22)")
    }

    @Test
    fun totalValue_withAdjustedDice_ignoresAdjustments() {
        // Arrange
        val dieValues = listOf(
            DieValue(6, 3),  // d6 with value 3
            DieValue(8, 5)   // d8 with value 5
        )
        val values = DieValues(dieValues)
        val adjusted = listOf(
            Adjusted.ByAmount(dieValues[0], 2),   // This adjustment should not affect totalValue
            Adjusted.ToMax(dieValues[1])          // This adjustment should not affect totalValue
        )
        val addToTotal = 4
        val combination = Combination(
            values = values,
            addToTotal = addToTotal,
            adjusted = adjusted
        )

        // Act
        val result = combination.totalValue

        // Assert
        assertEquals(12, result, "Total value should be sum of original die values plus addToTotal, ignoring adjustments (3 + 5 + 4 = 12)")
    }
} 