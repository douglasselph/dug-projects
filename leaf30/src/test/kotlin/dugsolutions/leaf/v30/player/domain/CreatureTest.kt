package dugsolutions.leaf.v30.player.domain

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Commons
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreatureTest {

    private lateinit var cards: List<GameCard>
    private lateinit var creature: Creature

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        cards = registry.getAllCards()
        creature = Creature()
    }

    @Test
    fun left_whenNew_isEmpty() {
        assertTrue(creature.isLeftEmpty)
    }

    @Test
    fun right_whenNew_isEmpty() {
        assertTrue(creature.isRightEmpty)
    }

    @Test
    fun addLeft_addsCardToLeftStackOnly() {
        // Arrange
        val card = cards[0]
        val expected = CreatureCard(card)

        // Act
        val result = creature.addLeft(card)

        // Assert
        assertEquals(listOf(expected), result.all)
        assertEquals(listOf(expected), creature.leftCards)
        assertTrue(creature.getLeft(0)!!.isFaceDown)
        assertTrue(creature.isRightEmpty)
    }

    @Test
    fun addRight_addsCardToRightStackOnly() {
        // Arrange
        val card = cards[0]
        val expected = CreatureCard(card)

        // Act
        val result = creature.addRight(card)

        // Assert
        assertEquals(listOf(expected), result.all)
        assertEquals(listOf(expected), creature.rightCards)
        assertTrue(creature.getRight(0)!!.isFaceDown)
        assertTrue(creature.isLeftEmpty)
    }

    @Test
    fun addLeft_withMultipleCards_preservesOrder() {
        // Arrange
        val first = cards[0]
        val second = cards[1]

        // Act
        creature.addLeft(first)
        creature.addLeft(second)

        // Assert
        assertEquals(listOf(CreatureCard(first), CreatureCard(second)), creature.leftCards)
    }

    @Test
    fun addRight_withMultipleCards_preservesOrder() {
        // Arrange
        val first = cards[0]
        val second = cards[1]

        // Act
        creature.addRight(first)
        creature.addRight(second)

        // Assert
        assertEquals(listOf(CreatureCard(first), CreatureCard(second)), creature.rightCards)
    }

    @Test
    fun addLeft_withCreatureCard_preservesFacing() {
        // Arrange
        val card = CreatureCard(cards[0], CreatureCard.Facing.FACE_UP)

        // Act
        creature.addLeft(card)

        // Assert
        assertEquals(listOf(card), creature.leftCards)
        assertTrue(creature.getLeft(0)!!.isFaceUp)
    }

}
