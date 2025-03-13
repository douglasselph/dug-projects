package dugsolutions.leaf.tool

import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ParseCostTest {

    private val SUT = ParseCost()

    @Test
    fun invoke_whenFree_returnsEmptyCost() {
        // Act
        val result = SUT("Free")

        // Assert
        assertCostEquals(Cost(emptyList()), result)
    }

    @Test
    fun invoke_whenEmpty_returnsEmptyCost() {
        // Act
        val result = SUT("")

        // Assert
        assertCostEquals(Cost(emptyList()), result)
    }

    @Test
    fun invoke_whenDash_returnsEmptyCost() {
        // Act
        val result = SUT("-")

        // Assert
        assertCostEquals(Cost(emptyList()), result)
    }

    @Test
    fun invoke_whenZero_returnsEmptyCost() {
        // Act
        val result = SUT("0")

        // Assert
        assertCostEquals(Cost(emptyList()), result)
    }

    @Test
    fun invoke_whenSingleDieMinimum_returnsCorrectCost() {
        // Act
        val result = SUT("S6+")

        // Assert
        assertCostEquals(
            Cost(listOf(CostElement.SingleDieMinimum(6))),
            result
        )
    }

    @Test
    fun invoke_whenSingleDieExact_returnsCorrectCost() {
        // Act
        val result = SUT("S4")

        // Assert
        assertCostEquals(
            Cost(listOf(CostElement.SingleDieExact(4))),
            result
        )
    }

    @Test
    fun invoke_whenTotalDiceMinimum_returnsCorrectCost() {
        // Act
        val result = SUT("M10+")

        // Assert
        assertCostEquals(
            Cost(listOf(CostElement.TotalDiceMinimum(10))),
            result
        )
    }

    @Test
    fun invoke_whenPlainNumber_returnsTotalDiceMinimum() {
        // Act
        val result = SUT("8")

        // Assert
        assertCostEquals(
            Cost(listOf(CostElement.TotalDiceMinimum(8))),
            result
        )
    }

    @Test
    fun invoke_whenMultipleElementsWithPlainNumber_returnsCorrectCost() {
        // Act
        val result = SUT("8,R,S6+")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostElement.TotalDiceMinimum(8),
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.SingleDieMinimum(6)
            )),
            result
        )
    }

    @Test
    fun invoke_whenTotalDiceExact_returnsCorrectCost() {
        // Act
        val result = SUT("M8")

        // Assert
        assertCostEquals(
            Cost(listOf(CostElement.TotalDiceExact(8))),
            result
        )
    }

    @Test
    fun invoke_whenFlourishType_returnsCorrectCost() {
        // Act
        val result = SUT("R")

        // Assert
        assertCostEquals(
            Cost(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT))),
            result
        )
    }

    @Test
    fun invoke_whenMultipleElements_returnsCorrectCost() {
        // Act
        val result = SUT("S6+,M10+,R")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostElement.SingleDieMinimum(6),
                CostElement.TotalDiceMinimum(10),
                CostElement.FlourishTypePresent(FlourishType.ROOT)
            )),
            result
        )
    }

    @Test
    fun invoke_whenMultipleElements2_returnsCorrectCost() {
        // Act
        val result = SUT("S6+,10,R")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostElement.SingleDieMinimum(6),
                CostElement.TotalDiceMinimum(10),
                CostElement.FlourishTypePresent(FlourishType.ROOT)
            )),
            result
        )
    }

    @Test
    fun invoke_whenInvalidElement_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            SUT("Invalid")
        }
    }

    @Test
    fun invoke_whenInvalidFlourishType_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            SUT("X")
        }
    }

    @Test
    fun invoke_whenInvalidDieFormat_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            SUT("S6++")
        }
    }

    @Test
    fun invoke_whenInvalidTotalFormat_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            SUT("M10++")
        }
    }

    @Test
    fun invoke_whenAllFlourishTypes_returnsCorrectCost() {
        // Act
        val result = SUT("R,C,V,B")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.FlourishTypePresent(FlourishType.CANOPY),
                CostElement.FlourishTypePresent(FlourishType.VINE),
                CostElement.FlourishTypePresent(FlourishType.BLOOM)
            )),
            result
        )
    }

    @Test
    fun invoke_whenComplexCombination_returnsCorrectCost() {
        // Act
        val result = SUT("S6+,M12,R,C")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostElement.SingleDieMinimum(6),
                CostElement.TotalDiceExact(12),
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.FlourishTypePresent(FlourishType.CANOPY)
            )),
            result
        )
    }

    private fun assertCostEquals(expected: Cost, actual: Cost) {
        assertEquals(expected.elements.size, actual.elements.size, "Cost elements count mismatch")
        expected.elements.forEachIndexed { index, expectedElement ->
            val actualElement = actual.elements[index]
            assertEquals(expectedElement, actualElement, "Cost element at index $index mismatch")
        }
    }
} 
