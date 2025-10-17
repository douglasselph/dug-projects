package dugsolutions.leaf.cards.cost

import dugsolutions.leaf.cards.domain.FlourishType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ParseCostElementTest {

    private val SUT = ParseCostElement()

    @Test
    fun invoke_whenSingleDieMinimum_returnsCorrectElement() {
        // Act
        val result = SUT("S6+")

        // Assert
        assertEquals(CostElement.SingleDieMinimum(6), result)
    }

    @Test
    fun invoke_whenSingleDieExact_returnsCorrectElement() {
        // Act
        val result = SUT("S4")

        // Assert
        assertEquals(CostElement.SingleDieExact(4), result)
    }

    @Test
    fun invoke_whenTotalDiceMinimum_returnsCorrectElement() {
        // Act
        val result = SUT("M10+")

        // Assert
        assertEquals(CostElement.TotalDiceMinimum(10), result)
    }

    @Test
    fun invoke_whenPlainNumber_returnsTotalDiceMinimum() {
        // Act
        val result = SUT("8")

        // Assert
        assertEquals(CostElement.TotalDiceMinimum(8), result)
    }

    @Test
    fun invoke_whenTotalDiceExact_returnsCorrectElement() {
        // Act
        val result = SUT("M8")

        // Assert
        assertEquals(CostElement.TotalDiceExact(8), result)
    }

    @Test
    fun invoke_whenRootFlourishType_returnsCorrectElement() {
        // Act
        val result = SUT("R")

        // Assert
        assertEquals(CostElement.FlourishTypePresent(FlourishType.ROOT), result)
    }

    @Test
    fun invoke_whenCanopyFlourishType_returnsCorrectElement() {
        // Act
        val result = SUT("C")

        // Assert
        assertEquals(CostElement.FlourishTypePresent(FlourishType.CANOPY), result)
    }

    @Test
    fun invoke_whenVineFlourishType_returnsCorrectElement() {
        // Act
        val result = SUT("V")

        // Assert
        assertEquals(CostElement.FlourishTypePresent(FlourishType.VINE), result)
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
            SUT("S6+++")
        }
    }

    @Test
    fun invoke_whenInvalidTotalFormat_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            SUT("M10++")
        }
    }

} 
