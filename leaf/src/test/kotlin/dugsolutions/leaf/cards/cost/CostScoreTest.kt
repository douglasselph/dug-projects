package dugsolutions.leaf.cards.cost

import dugsolutions.leaf.cards.domain.FlourishType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CostScoreTest {

    private val SUT = CostScore()

    @Test
    fun invoke_whenEmptyCost_returnsZero() {
        // Arrange
        val cost = Cost(emptyList())

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun invoke_whenSingleDieMinimum_returnsCorrectScore() {
        // Arrange
        val cost = Cost(listOf(
            CostAlternative(listOf(CostElement.SingleDieMinimum(6)))
        ))

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(11, result) // 6 + 5
    }

    @Test
    fun invoke_whenSingleDieExact_returnsCorrectScore() {
        // Arrange
        val cost = Cost(listOf(
            CostAlternative(listOf(CostElement.SingleDieExact(4)))
        ))

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(12, result) // 4 * 3
    }

    @Test
    fun invoke_whenTotalDiceMinimum_returnsCorrectScore() {
        // Arrange
        val cost = Cost(listOf(
            CostAlternative(listOf(CostElement.TotalDiceMinimum(10)))
        ))

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(10, result)
    }

    @Test
    fun invoke_whenTotalDiceExact_returnsCorrectScore() {
        // Arrange
        val cost = Cost(listOf(
            CostAlternative(listOf(CostElement.TotalDiceExact(8)))
        ))

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(24, result) // 8 * 3
    }

    @Test
    fun invoke_whenFlourishType_returnsCorrectScore() {
        // Arrange
        val cost = Cost(listOf(
            CostAlternative(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT)))
        ))

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(7, result)
    }

    @Test
    fun invoke_whenMultipleElements_returnsSumOfScores() {
        // Arrange
        val cost = Cost(listOf(
            CostAlternative(listOf(
                CostElement.SingleDieMinimum(6),
                CostElement.TotalDiceMinimum(10),
                CostElement.FlourishTypePresent(FlourishType.ROOT)
            ))
        ))

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(28, result) // (6 + 5) + 10 + 7
    }

    @Test
    fun invoke_whenSimpleOrCost_returnsMinimumScore() {
        // Arrange
        val cost = Cost(listOf(
            CostAlternative(listOf(
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.TotalDiceMinimum(8)
            )),
            CostAlternative(listOf(
                CostElement.TotalDiceMinimum(15)
            ))
        ))

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(15, result) // min(7 + 8, 15)
    }

    @Test
    fun invoke_whenComplexOrCost_returnsMinimumScore() {
        // Arrange
        val cost = Cost(listOf(
            CostAlternative(listOf(
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.SingleDieMinimum(6),
                CostElement.TotalDiceMinimum(8)
            )),
            CostAlternative(listOf(
                CostElement.FlourishTypePresent(FlourishType.CANOPY),
                CostElement.TotalDiceExact(15)
            ))
        ))

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(26, result) // min((7 + 11 + 8), (7 + 45))
    }

    @Test
    fun invoke_whenMultipleAlternatives_returnsLowestScore() {
        // Arrange
        val cost = Cost(listOf(
            CostAlternative(listOf(
                CostElement.TotalDiceMinimum(20)
            )),
            CostAlternative(listOf(
                CostElement.SingleDieMinimum(8),
                CostElement.FlourishTypePresent(FlourishType.ROOT)
            )),
            CostAlternative(listOf(
                CostElement.TotalDiceExact(10)
            ))
        ))

        // Act
        val result = SUT(cost)

        // Assert
        assertEquals(20, result) // min(20, (13 + 7), 30)
    }
} 
