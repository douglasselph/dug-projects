package dugsolutions.leaf.cards.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CardEffectTest {

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
    fun values_whenCalled_returnsAllCardEffects() {
        // Arrange
        val expectedEffects = listOf(
            CardEffect.NONE,
            CardEffect.ADD_TO_DIE,
            CardEffect.GRAFT_DIE,
            CardEffect.REROLL_ACCEPT_2ND,
            CardEffect.UPGRADE
        )

        // Act
        val actualEffects = CardEffect.entries

        // Assert
        assertEquals(expectedEffects, actualEffects)
    }

    @Test
    fun name_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(EXPECTED_NONE, CardEffect.NONE.name)
        assertEquals(EXPECTED_ADD_TO_DIE, CardEffect.ADD_TO_DIE.name)
        assertEquals(EXPECTED_GRAFT_DIE, CardEffect.GRAFT_DIE.name)
        assertEquals(EXPECTED_REROLL_ACCEPT_2ND, CardEffect.REROLL_ACCEPT_2ND.name)
        assertEquals(EXPECTED_UPGRADE, CardEffect.UPGRADE.name)
    }

    @Test
    fun match_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(MATCH_NONE, CardEffect.NONE.match)
        assertEquals(MATCH_ADD_TO_DIE, CardEffect.ADD_TO_DIE.match)
        assertEquals(MATCH_GRAFT_DIE, CardEffect.GRAFT_DIE.match)
        assertEquals(MATCH_REROLL_ACCEPT_2ND, CardEffect.REROLL_ACCEPT_2ND.match)
        assertEquals(MATCH_UPGRADE, CardEffect.UPGRADE.match)
    }

    @Test
    fun description_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(DESCRIPTION_NONE, CardEffect.NONE.description)
        assertEquals(DESCRIPTION_ADD_TO_DIE, CardEffect.ADD_TO_DIE.description)
        assertEquals(DESCRIPTION_GRAFT_DIE, CardEffect.GRAFT_DIE.description)
        assertEquals(DESCRIPTION_REROLL_ACCEPT_2ND, CardEffect.REROLL_ACCEPT_2ND.description)
        assertEquals(DESCRIPTION_UPGRADE, CardEffect.UPGRADE.description)
    }

    @Test
    fun from_whenValidMatch_returnsCorrectCardEffect() {
        // Arrange & Act & Assert
        assertEquals(CardEffect.NONE, CardEffect.from(MATCH_NONE))
        assertEquals(CardEffect.ADD_TO_DIE, CardEffect.from(MATCH_ADD_TO_DIE))
        assertEquals(CardEffect.GRAFT_DIE, CardEffect.from(MATCH_GRAFT_DIE))
        assertEquals(CardEffect.REROLL_ACCEPT_2ND, CardEffect.from(MATCH_REROLL_ACCEPT_2ND))
        assertEquals(CardEffect.UPGRADE, CardEffect.from(MATCH_UPGRADE))
    }

    @Test
    fun from_whenNoMatch_returnsNull() {
        // Arrange
        val invalidInput = "InvalidEffect"

        // Act
        val result = CardEffect.from(invalidInput)

        // Assert
        assertNull(result)
    }

    @Test
    fun from_whenEmptyString_returnsNull() {
        // Arrange
        val emptyInput = ""

        // Act
        val result = CardEffect.from(emptyInput)

        // Assert
        assertNull(result)
    }

    @Test
    fun from_whenCaseMismatch_returnsNull() {
        // Arrange
        val caseMismatch = "addtodie"

        // Act
        val result = CardEffect.from(caseMismatch)

        // Assert
        assertNull(result)
    }

    @Test
    fun from_whenPartialMatch_returnsNull() {
        // Arrange
        val partialMatch = "AddTo"

        // Act
        val result = CardEffect.from(partialMatch)

        // Assert
        assertNull(result)
    }
}
