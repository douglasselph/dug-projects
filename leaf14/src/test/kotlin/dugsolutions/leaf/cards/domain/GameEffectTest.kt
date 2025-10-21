package dugsolutions.leaf.cards.domain

import dugsolutions.leaf.common.domain.GameEffect
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GameEffectTest {

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
        assertEquals(EXPECTED_NONE, GameEffect.NONE.name)
        assertEquals(EXPECTED_ADD_TO_DIE, GameEffect.ADD_TO_DIE.name)
        assertEquals(EXPECTED_GRAFT_DIE, GameEffect.GRAFT_DIE.name)
        assertEquals(EXPECTED_REROLL_ACCEPT_2ND, GameEffect.REROLL_ACCEPT_2ND.name)
        assertEquals(EXPECTED_UPGRADE, GameEffect.UPGRADE.name)
    }

    @Test
    fun match_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(MATCH_NONE, GameEffect.NONE.match)
        assertEquals(MATCH_ADD_TO_DIE, GameEffect.ADD_TO_DIE.match)
        assertEquals(MATCH_GRAFT_DIE, GameEffect.GRAFT_DIE.match)
        assertEquals(MATCH_REROLL_ACCEPT_2ND, GameEffect.REROLL_ACCEPT_2ND.match)
        assertEquals(MATCH_UPGRADE, GameEffect.UPGRADE.match)
    }

    @Test
    fun description_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(DESCRIPTION_NONE, GameEffect.NONE.description)
        assertEquals(DESCRIPTION_ADD_TO_DIE, GameEffect.ADD_TO_DIE.description)
        assertEquals(DESCRIPTION_GRAFT_DIE, GameEffect.GRAFT_DIE.description)
        assertEquals(DESCRIPTION_REROLL_ACCEPT_2ND, GameEffect.REROLL_ACCEPT_2ND.description)
        assertEquals(DESCRIPTION_UPGRADE, GameEffect.UPGRADE.description)
    }

    @Test
    fun from_whenValidMatch_returnsCorrectCardEffect() {
        // Arrange & Act & Assert
        assertEquals(GameEffect.NONE, GameEffect.from(MATCH_NONE))
        assertEquals(GameEffect.ADD_TO_DIE, GameEffect.from(MATCH_ADD_TO_DIE))
        assertEquals(GameEffect.GRAFT_DIE, GameEffect.from(MATCH_GRAFT_DIE))
        assertEquals(GameEffect.REROLL_ACCEPT_2ND, GameEffect.from(MATCH_REROLL_ACCEPT_2ND))
        assertEquals(GameEffect.UPGRADE, GameEffect.from(MATCH_UPGRADE))
    }

    @Test
    fun from_whenNoMatch_returnsNull() {
        // Arrange
        val invalidInput = "InvalidEffect"

        // Act
        val result = GameEffect.from(invalidInput)

        // Assert
        assertNull(result)
    }

    @Test
    fun from_whenEmptyString_returnsNull() {
        // Arrange
        val emptyInput = ""

        // Act
        val result = GameEffect.from(emptyInput)

        // Assert
        assertNull(result)
    }

    @Test
    fun from_whenCaseMismatch_returnsNull() {
        // Arrange
        val caseMismatch = "addtodie"

        // Act
        val result = GameEffect.from(caseMismatch)

        // Assert
        assertNull(result)
    }

    @Test
    fun from_whenPartialMatch_returnsNull() {
        // Arrange
        val partialMatch = "AddTo"

        // Act
        val result = GameEffect.from(partialMatch)

        // Assert
        assertNull(result)
    }
}
