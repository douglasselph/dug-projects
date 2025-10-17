package dugsolutions.leaf.cards.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CardEffectTest {

    companion object {
        private const val EXPECTED_NONE = "NONE"
        private const val MATCH_NONE = "None"
        private const val DESCRIPTION_NONE = "This card has no special effect"
    }

    @Test
    fun values_whenCalled_returnsAllCardEffects() {
        // Arrange
        val expectedEffects = listOf(CardEffect.NONE)

        // Act
        val actualEffects = CardEffect.entries

        // Assert
        assertEquals(expectedEffects, actualEffects)
    }

    @Test
    fun name_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(EXPECTED_NONE, CardEffect.NONE.name)
    }

    @Test
    fun match_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(MATCH_NONE, CardEffect.NONE.match)
    }

    @Test
    fun description_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(DESCRIPTION_NONE, CardEffect.NONE.description)
    }

    @Test
    fun from_whenValidMatch_returnsCorrectCardEffect() {
        // Arrange & Act & Assert
        assertEquals(CardEffect.NONE, CardEffect.from(MATCH_NONE))
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
        val caseMismatch = "none"

        // Act
        val result = CardEffect.from(caseMismatch)

        // Assert
        assertNull(result)
    }
}
