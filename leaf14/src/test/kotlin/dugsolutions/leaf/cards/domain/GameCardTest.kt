package dugsolutions.leaf.cards.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GameCardTest {

    companion object {
        private const val CARD_ID = 1
        private const val CARD_NAME = "Test Card"
        private const val CARD_IMAGE = "test-image.png"
        private const val CARD_COUNT = 3
        private const val CARD_NOTES = "Test notes"
        private const val RESILIENCE = 5
        private const val NUTRIENT = 3
        private const val PRIMARY_VALUE = 2
        private const val MATCH_VALUE = 1
    }

    @Test
    fun constructor_whenAllPropertiesProvided_createsGameCard() {
        // Arrange
        val id = CARD_ID
        val name = CARD_NAME
        val type = FlourishType.CANOPY
        val resilience = RESILIENCE
        val nutrient = NUTRIENT
        val cost = Cost.Value(2)
        val phase = Phase.Cultivation
        val primaryEffect = CardEffect.NONE
        val primaryValue = PRIMARY_VALUE
        val matchWith = MatchWith.Sap
        val matchEffect = CardEffect.NONE
        val matchValue = MATCH_VALUE
        val image = CARD_IMAGE
        val count = CARD_COUNT
        val notes = CARD_NOTES

        // Act
        val gameCard = GameCard(
            id = id,
            name = name,
            type = type,
            resilience = resilience,
            cost = cost,
            phase = phase,
            primaryEffect = primaryEffect,
            primaryValue = primaryValue,
            matchWith = matchWith,
            matchEffect = matchEffect,
            matchValue = matchValue,
            image = image,
            count = count,
            notes = notes
        )

        // Assert
        assertEquals(id, gameCard.id)
        assertEquals(name, gameCard.name)
        assertEquals(type, gameCard.type)
        assertEquals(resilience, gameCard.resilience)
        assertEquals(cost, gameCard.cost)
        assertEquals(phase, gameCard.phase)
        assertEquals(primaryEffect, gameCard.primaryEffect)
        assertEquals(primaryValue, gameCard.primaryValue)
        assertEquals(matchWith, gameCard.matchWith)
        assertEquals(matchEffect, gameCard.matchEffect)
        assertEquals(matchValue, gameCard.matchValue)
        assertEquals(image, gameCard.image)
        assertEquals(count, gameCard.count)
        assertEquals(notes, gameCard.notes)
    }

    @Test
    fun constructor_whenOptionalPropertiesNull_createsGameCardWithNulls() {
        // Arrange
        val id = CARD_ID
        val name = CARD_NAME
        val type = FlourishType.NONE
        val resilience = 0
        val nutrient = 0
        val cost = Cost.None
        val phase = Phase.Battle
        val primaryValue = 0
        val matchWith = MatchWith.None
        val matchValue = 0
        val count = 1

        // Act
        val gameCard = GameCard(
            id = id,
            name = name,
            type = type,
            resilience = resilience,
            cost = cost,
            phase = phase,
            primaryEffect = null,
            primaryValue = primaryValue,
            matchWith = matchWith,
            matchEffect = null,
            matchValue = matchValue,
            count = count
        )

        // Assert
        assertEquals(id, gameCard.id)
        assertEquals(name, gameCard.name)
        assertEquals(type, gameCard.type)
        assertEquals(resilience, gameCard.resilience)
        assertEquals(cost, gameCard.cost)
        assertEquals(phase, gameCard.phase)
        assertNull(gameCard.primaryEffect)
        assertEquals(primaryValue, gameCard.primaryValue)
        assertEquals(matchWith, gameCard.matchWith)
        assertNull(gameCard.matchEffect)
        assertEquals(matchValue, gameCard.matchValue)
        assertNull(gameCard.image)
        assertEquals(count, gameCard.count)
        assertNull(gameCard.notes)
    }

    @Test
    fun equals_whenSameProperties_returnsTrue() {
        // Arrange
        val card1 = createTestGameCard()
        val card2 = createTestGameCard()

        // Act & Assert
        assertEquals(card1, card2)
    }

    @Test
    fun equals_whenDifferentId_returnsFalse() {
        // Arrange
        val card1 = createTestGameCard(id = 1)
        val card2 = createTestGameCard(id = 2)

        // Act & Assert
        assertNotEquals(card1, card2)
    }

    @Test
    fun equals_whenDifferentName_returnsFalse() {
        // Arrange
        val card1 = createTestGameCard(name = "Card 1")
        val card2 = createTestGameCard(name = "Card 2")

        // Act & Assert
        assertNotEquals(card1, card2)
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        // Arrange
        val card1 = createTestGameCard(type = FlourishType.CANOPY)
        val card2 = createTestGameCard(type = FlourishType.ROOT)

        // Act & Assert
        assertNotEquals(card1, card2)
    }

    @Test
    fun equals_whenDifferentCost_returnsFalse() {
        // Arrange
        val card1 = createTestGameCard(cost = Cost.None)
        val card2 = createTestGameCard(cost = Cost.Value(2))

        // Act & Assert
        assertNotEquals(card1, card2)
    }

    @Test
    fun equals_whenSameInstance_returnsTrue() {
        // Arrange
        val card = createTestGameCard()

        // Act & Assert
        assertEquals(card, card)
    }

    @Test
    fun equals_whenDifferentClass_returnsFalse() {
        // Arrange
        val card = createTestGameCard()
        val other = "Not a GameCard"

        // Act & Assert
        assertNotEquals(card, other)
    }

    @Test
    fun hashCode_whenSameProperties_returnsSameHashCode() {
        // Arrange
        val card1 = createTestGameCard()
        val card2 = createTestGameCard()

        // Act & Assert
        assertEquals(card1.hashCode(), card2.hashCode())
    }

    @Test
    fun hashCode_whenDifferentProperties_returnsDifferentHashCode() {
        // Arrange
        val card1 = createTestGameCard(id = 1)
        val card2 = createTestGameCard(id = 2)

        // Act & Assert
        assertNotEquals(card1.hashCode(), card2.hashCode())
    }

    @Test
    fun toString_whenCalled_returnsCorrectString() {
        // Arrange
        val card = createTestGameCard()

        // Act
        val result = card.toString()

        // Assert
        assertTrue(result.contains("GameCard"))
        assertTrue(result.contains("id=$CARD_ID"))
        assertTrue(result.contains("name='$CARD_NAME'"))
        assertTrue(result.contains("type=${FlourishType.CANOPY}"))
        assertTrue(result.contains("resilience=$RESILIENCE"))
        assertTrue(result.contains("cost=${Cost.Value(2)}"))
        assertTrue(result.contains("phase=${Phase.Cultivation}"))
        assertTrue(result.contains("primaryEffect=${CardEffect.NONE}"))
        assertTrue(result.contains("primaryValue=$PRIMARY_VALUE"))
        assertTrue(result.contains("matchWith=${MatchWith.Sap}"))
        assertTrue(result.contains("matchEffect=${CardEffect.NONE}"))
        assertTrue(result.contains("matchValue=$MATCH_VALUE"))
        assertTrue(result.contains("image=$CARD_IMAGE"))
        assertTrue(result.contains("count=$CARD_COUNT"))
        assertTrue(result.contains("notes=$CARD_NOTES"))
    }

    @Test
    fun toString_whenNullProperties_handlesNullsCorrectly() {
        // Arrange
        val card = createTestGameCard(
            primaryEffect = null,
            matchEffect = null,
            image = null,
            notes = null
        )

        // Act
        val result = card.toString()

        // Assert
        assertTrue(result.contains("primaryEffect=null"))
        assertTrue(result.contains("matchEffect=null"))
        assertTrue(result.contains("image=null"))
        assertTrue(result.contains("notes=null"))
    }

    private fun createTestGameCard(
        id: Int = CARD_ID,
        name: String = CARD_NAME,
        type: FlourishType = FlourishType.CANOPY,
        resilience: Int = RESILIENCE,
        nutrient: Int = NUTRIENT,
        cost: Cost = Cost.Value(2),
        phase: Phase = Phase.Cultivation,
        primaryEffect: CardEffect? = CardEffect.NONE,
        primaryValue: Int = PRIMARY_VALUE,
        matchWith: MatchWith = MatchWith.Sap,
        matchEffect: CardEffect? = CardEffect.NONE,
        matchValue: Int = MATCH_VALUE,
        image: String? = CARD_IMAGE,
        count: Int = CARD_COUNT,
        notes: String? = CARD_NOTES
    ): GameCard {
        return GameCard(
            id = id,
            name = name,
            type = type,
            resilience = resilience,
            cost = cost,
            phase = phase,
            primaryEffect = primaryEffect,
            primaryValue = primaryValue,
            matchWith = matchWith,
            matchEffect = matchEffect,
            matchValue = matchValue,
            image = image,
            count = count,
            notes = notes
        )
    }
}
