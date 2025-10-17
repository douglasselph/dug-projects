package dugsolutions.leaf.cards.domain

import dugsolutions.leaf.common.domain.Effect
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EffectTest {

    companion object {
        private const val EXPECTED_NONE = "NONE"
        private const val EXPECTED_ADD_TO_DIE = "ADD_TO_DIE"
        private const val EXPECTED_GRAFT_DIE = "GRAFT_DIE"
        private const val EXPECTED_REROLL_ACCEPT_2ND = "REROLL_ACCEPT_2ND"
        private const val EXPECTED_UPGRADE = "UPGRADE"
        
        private const val MATCH_NONE = "-"
        private const val MATCH_ADD_TO_DIE = "AddToDie"
        private const val MATCH_GRAFT_DIE = "GraftDie"
        private const val MATCH_REROLL_ACCEPT_2ND = "RerollAccept2nd"
        private const val MATCH_UPGRADE = "Upgrade"
        
        private const val DESCRIPTION_NONE = "No effect"
        private const val DESCRIPTION_ADD_TO_DIE = "Add VALUE to one die, without exceeding MAX"
        private const val DESCRIPTION_GRAFT_DIE = "Add a grafted die to the Canopy"
        private const val DESCRIPTION_REROLL_ACCEPT_2ND = "Reroll VALUE dice, you must accept the second roll"
        private const val DESCRIPTION_UPGRADE = "Upgrade VALUE dice then discard."
    }

    @Test
    fun name_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(EXPECTED_NONE, Effect.NONE.name)
        assertEquals(EXPECTED_ADD_TO_DIE, Effect.ADD_TO_DIE.name)
        assertEquals(EXPECTED_GRAFT_DIE, Effect.GRAFT_DIE.name)
        assertEquals(EXPECTED_REROLL_ACCEPT_2ND, Effect.REROLL_ACCEPT_2ND.name)
        assertEquals(EXPECTED_UPGRADE, Effect.UPGRADE.name)
    }

    @Test
    fun match_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(MATCH_NONE, Effect.NONE.match)
        assertEquals(MATCH_ADD_TO_DIE, Effect.ADD_TO_DIE.match)
        assertEquals(MATCH_GRAFT_DIE, Effect.GRAFT_DIE.match)
        assertEquals(MATCH_REROLL_ACCEPT_2ND, Effect.REROLL_ACCEPT_2ND.match)
        assertEquals(MATCH_UPGRADE, Effect.UPGRADE.match)
    }

    @Test
    fun description_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(DESCRIPTION_NONE, Effect.NONE.description)
        assertEquals(DESCRIPTION_ADD_TO_DIE, Effect.ADD_TO_DIE.description)
        assertEquals(DESCRIPTION_GRAFT_DIE, Effect.GRAFT_DIE.description)
        assertEquals(DESCRIPTION_REROLL_ACCEPT_2ND, Effect.REROLL_ACCEPT_2ND.description)
        assertEquals(DESCRIPTION_UPGRADE, Effect.UPGRADE.description)
    }

    @Test
    fun from_whenValidMatch_returnsCorrectCardEffect() {
        // Arrange & Act & Assert
        assertEquals(Effect.NONE, Effect.from(MATCH_NONE))
        assertEquals(Effect.ADD_TO_DIE, Effect.from(MATCH_ADD_TO_DIE))
        assertEquals(Effect.GRAFT_DIE, Effect.from(MATCH_GRAFT_DIE))
        assertEquals(Effect.REROLL_ACCEPT_2ND, Effect.from(MATCH_REROLL_ACCEPT_2ND))
        assertEquals(Effect.UPGRADE, Effect.from(MATCH_UPGRADE))
    }

    @Test
    fun from_whenNoMatch_returnsNull() {
        // Arrange
        val invalidInput = "InvalidEffect"

        // Act
        val result = Effect.from(invalidInput)

        // Assert
        assertNull(result)
    }

    @Test
    fun from_whenEmptyString_returnsNull() {
        // Arrange
        val emptyInput = ""

        // Act
        val result = Effect.from(emptyInput)

        // Assert
        assertNull(result)
    }

    @Test
    fun from_whenCaseMismatch_returnsNull() {
        // Arrange
        val caseMismatch = "addtodie"

        // Act
        val result = Effect.from(caseMismatch)

        // Assert
        assertNull(result)
    }

    @Test
    fun from_whenPartialMatch_returnsNull() {
        // Arrange
        val partialMatch = "AddTo"

        // Act
        val result = Effect.from(partialMatch)

        // Assert
        assertNull(result)
    }
}
