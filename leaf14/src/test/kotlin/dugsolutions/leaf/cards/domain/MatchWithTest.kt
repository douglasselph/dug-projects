package dugsolutions.leaf.cards.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MatchWithTest {

    companion object {
        private const val EXPECTED_NONE = "None"
        private const val EXPECTED_PULLED_GRAFT = "PulledGraft"
        private const val EXPECTED_WORM_OR_SAP = "WormOrSap"
        private const val EXPECTED_SAP = "Sap"
        private const val EXPECTED_BEE = "Bee"
        private const val EXPECTED_END = "End"
        
        private const val MATCH_NONE = "-"
        private const val MATCH_PULLED_GRAFT = "PulledGraft"
        private const val MATCH_WORM_OR_SAP = "Worm|Sap"
        private const val MATCH_SAP = "Sap"
        private const val MATCH_BEE = "Bee"
        private const val MATCH_END = "End"
    }

    @Test
    fun values_whenCalled_returnsAllMatchWithTypes() {
        // Arrange
        val expectedTypes = listOf(
            MatchWith.None,
            MatchWith.PulledGraft,
            MatchWith.WormOrSap,
            MatchWith.Sap,
            MatchWith.Bee,
            MatchWith.End
        )

        // Act
        val actualTypes = MatchWith.entries

        // Assert
        assertEquals(expectedTypes, actualTypes)
    }

    @Test
    fun name_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(EXPECTED_NONE, MatchWith.None.name)
        assertEquals(EXPECTED_PULLED_GRAFT, MatchWith.PulledGraft.name)
        assertEquals(EXPECTED_WORM_OR_SAP, MatchWith.WormOrSap.name)
        assertEquals(EXPECTED_SAP, MatchWith.Sap.name)
        assertEquals(EXPECTED_BEE, MatchWith.Bee.name)
        assertEquals(EXPECTED_END, MatchWith.End.name)
    }

    @Test
    fun match_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(MATCH_NONE, MatchWith.None.match)
        assertEquals(MATCH_PULLED_GRAFT, MatchWith.PulledGraft.match)
        assertEquals(MATCH_WORM_OR_SAP, MatchWith.WormOrSap.match)
        assertEquals(MATCH_SAP, MatchWith.Sap.match)
        assertEquals(MATCH_BEE, MatchWith.Bee.match)
        assertEquals(MATCH_END, MatchWith.End.match)
    }

    @Test
    fun from_whenValidMatch_returnsCorrectMatchWith() {
        // Arrange & Act & Assert
        assertEquals(MatchWith.None, MatchWith.from(MATCH_NONE))
        assertEquals(MatchWith.PulledGraft, MatchWith.from(MATCH_PULLED_GRAFT))
        assertEquals(MatchWith.WormOrSap, MatchWith.from(MATCH_WORM_OR_SAP))
        assertEquals(MatchWith.Sap, MatchWith.from(MATCH_SAP))
        assertEquals(MatchWith.Bee, MatchWith.from(MATCH_BEE))
        assertEquals(MatchWith.End, MatchWith.from(MATCH_END))
    }

    @Test
    fun from_whenCaseInsensitive_returnsCorrectMatchWith() {
        // Arrange & Act & Assert
        assertEquals(MatchWith.PulledGraft, MatchWith.from("pulledgraft"))
        assertEquals(MatchWith.PulledGraft, MatchWith.from("PULLEDGRAFT"))
        assertEquals(MatchWith.PulledGraft, MatchWith.from("PulledGraft"))
        assertEquals(MatchWith.Sap, MatchWith.from("sap"))
        assertEquals(MatchWith.Sap, MatchWith.from("SAP"))
        assertEquals(MatchWith.Bee, MatchWith.from("bee"))
        assertEquals(MatchWith.Bee, MatchWith.from("BEE"))
        assertEquals(MatchWith.End, MatchWith.from("end"))
        assertEquals(MatchWith.End, MatchWith.from("END"))
    }

    @Test
    fun from_whenStartsWithMatch_returnsCorrectMatchWith() {
        // Arrange & Act & Assert
        assertEquals(MatchWith.PulledGraft, MatchWith.from("PulledGraft Card"))
        assertEquals(MatchWith.Sap, MatchWith.from("Sap Tree"))
        assertEquals(MatchWith.Bee, MatchWith.from("Bee Hive"))
        assertEquals(MatchWith.End, MatchWith.from("End Game"))
        assertEquals(MatchWith.WormOrSap, MatchWith.from("Worm|Sap Effect"))
    }

    @Test
    fun from_whenNoMatch_throwsIllegalArgumentException() {
        // Arrange
        val invalidInput = "InvalidMatch"

        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MatchWith.from(invalidInput)
        }
        assertEquals("No matching MatchWith found for: $invalidInput", exception.message)
    }

    @Test
    fun from_whenEmptyString_returnsNone() {
        // Arrange & Act
        val result = MatchWith.from("")

        // Assert
        assertEquals(MatchWith.None, result)
    }

    @Test
    fun from_whenPartialMatch_returnsCorrectMatchWith() {
        // Arrange & Act & Assert
        assertEquals(MatchWith.Sap, MatchWith.from("Sap"))
        assertEquals(MatchWith.Bee, MatchWith.from("Bee"))
        assertEquals(MatchWith.End, MatchWith.from("End"))
    }
}
