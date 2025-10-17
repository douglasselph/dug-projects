package dugsolutions.leaf.cards.cost

import dugsolutions.leaf.cards.domain.FlourishType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ParseCostTest {

    private val parseCostElement = ParseCostElement()
    private val SUT = ParseCost(parseCostElement)

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
            Cost(listOf(CostAlternative(listOf(CostElement.SingleDieMinimum(6))))),
            result
        )
    }

    @Test
    fun invoke_whenSingleDieExact_returnsCorrectCost() {
        // Act
        val result = SUT("S4")

        // Assert
        assertCostEquals(
            Cost(listOf(CostAlternative(listOf(CostElement.SingleDieExact(4))))),
            result
        )
    }

    @Test
    fun invoke_whenTotalDiceMinimum_returnsCorrectCost() {
        // Act
        val result = SUT("M10+")

        // Assert
        assertCostEquals(
            Cost(listOf(CostAlternative(listOf(CostElement.TotalDiceMinimum(10))))),
            result
        )
    }

    @Test
    fun invoke_whenRootMinimumCost_returnsCorrectCost() {
        // Act
        val result = SUT("R 9")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostAlternative(listOf(
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.TotalDiceMinimum(9)
            ))
            )),
            result
        )
    }

    @Test
    fun invoke_whenPlainNumber_returnsTotalDiceMinimum() {
        // Act
        val result = SUT("8")

        // Assert
        assertCostEquals(
            Cost(listOf(CostAlternative(listOf(CostElement.TotalDiceMinimum(8))))),
            result
        )
    }

    @Test
    fun invoke_whenMultipleElementsWithPlainNumber_returnsCorrectCost() {
        // Act
        val result = SUT("8 R S6+")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostAlternative(listOf(
                CostElement.TotalDiceMinimum(8),
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.SingleDieMinimum(6)
            ))
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
            Cost(listOf(CostAlternative(listOf(CostElement.TotalDiceExact(8))))),
            result
        )
    }

    @Test
    fun invoke_whenFlourishType_returnsCorrectCost() {
        // Act
        val result = SUT("R")

        // Assert
        assertCostEquals(
            Cost(listOf(CostAlternative(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT))))),
            result
        )
    }

    @Test
    fun invoke_whenMultipleElements_returnsCorrectCost() {
        // Act
        val result = SUT("S6+ M10+ R")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostAlternative(listOf(
                CostElement.SingleDieMinimum(6),
                CostElement.TotalDiceMinimum(10),
                CostElement.FlourishTypePresent(FlourishType.ROOT)
            ))
            )),
            result
        )
    }

    @Test
    fun invoke_whenMultipleElements2_returnsCorrectCost() {
        // Act
        val result = SUT("S6+ 10 R")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostAlternative(listOf(
                CostElement.SingleDieMinimum(6),
                CostElement.TotalDiceMinimum(10),
                CostElement.FlourishTypePresent(FlourishType.ROOT)
            ))
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
    fun invoke_whenAllFlourishTypes_returnsCorrectCost() {
        // Act
        val result = SUT("R C V")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostAlternative(listOf(
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.FlourishTypePresent(FlourishType.CANOPY),
                CostElement.FlourishTypePresent(FlourishType.VINE)
            ))
            )),
            result
        )
    }

    @Test
    fun invoke_whenComplexCombination_returnsCorrectCost() {
        // Act
        val result = SUT("S6+ M12 R C")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostAlternative(listOf(
                CostElement.SingleDieMinimum(6),
                CostElement.TotalDiceExact(12),
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.FlourishTypePresent(FlourishType.CANOPY)
            ))
            )),
            result
        )
    }

    @Test
    fun invoke_whenOrCost_returnsCorrectCost() {
        // Act
        val result = SUT("R 8|15")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostAlternative(listOf(
                    CostElement.FlourishTypePresent(FlourishType.ROOT),
                    CostElement.TotalDiceMinimum(8)
                )),
                CostAlternative(listOf(
                    CostElement.TotalDiceMinimum(15)
                ))
            )),
            result
        )
    }

    @Test
    fun invoke_whenComplexOrCost_returnsCorrectCost() {
        // Act
        val result = SUT("R S6+ 8|C M15")

        // Assert
        assertCostEquals(
            Cost(listOf(
                CostAlternative(listOf(
                    CostElement.FlourishTypePresent(FlourishType.ROOT),
                    CostElement.SingleDieMinimum(6),
                    CostElement.TotalDiceMinimum(8)
                )),
                CostAlternative(listOf(
                    CostElement.FlourishTypePresent(FlourishType.CANOPY),
                    CostElement.TotalDiceExact(15)
                ))
            )),
            result
        )
    }

    private fun assertCostEquals(expected: Cost, actual: Cost) {
        assertEquals(expected.alternatives.size, actual.alternatives.size, "Cost alternatives count mismatch")
        expected.alternatives.forEachIndexed { altIndex, expectedAlternative ->
            val actualAlternative = actual.alternatives[altIndex]
            assertEquals(
                expectedAlternative.elements.size,
                actualAlternative.elements.size,
                "Alternative $altIndex elements count mismatch"
            )
            expectedAlternative.elements.forEachIndexed { elemIndex, expectedElement ->
                val actualElement = actualAlternative.elements[elemIndex]
                assertEquals(
                    expectedElement,
                    actualElement,
                    "Alternative $altIndex, element $elemIndex mismatch"
                )
            }
        }
    }
} 
