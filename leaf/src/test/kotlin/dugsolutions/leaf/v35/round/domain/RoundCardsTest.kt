package dugsolutions.leaf.v35.round.domain

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.random.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoundCardsTest {

    private lateinit var sourceCards: List<RoundCard>
    private lateinit var roundCards: RoundCards

    @BeforeEach
    fun setup() {
        sourceCards = listOf(
            roundCard("First", RoundCardType.BATTLE),
            roundCard("Second", RoundCardType.CULTIVATION),
            roundCard("Third", RoundCardType.BATTLE)
        )
        roundCards = RoundCards(sourceCards)
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCards() {
        // Arrange
        val mutableCards = sourceCards.toMutableList()
        val result = RoundCards(mutableCards)

        // Act
        mutableCards.clear()

        // Assert
        assertEquals(sourceCards, result.cards)
    }

    @Test
    fun size_returnsCorrectNumberOfCards() {
        // Act
        val result = roundCards.size

        // Assert
        assertEquals(sourceCards.size, result)
    }

    @Test
    fun iterator_returnsCardsInOrder() {
        // Act
        val result = roundCards.toList()

        // Assert
        assertEquals(sourceCards, result)
    }

    @Test
    fun get_whenIndexValid_returnsCard() {
        // Act
        val result = roundCards[1]

        // Assert
        assertEquals(sourceCards[1], result)
    }

    @Test
    fun take_returnsFirstNCards() {
        // Act
        val result = roundCards.take(2)

        // Assert
        assertEquals(sourceCards.take(2), result.cards)
    }

    @Test
    fun plus_combinesTwoRoundCardsCollections() {
        // Arrange
        val first = RoundCards(sourceCards.take(1))
        val second = RoundCards(sourceCards.drop(1))

        // Act
        val result = first + second

        // Assert
        assertEquals(sourceCards, result.cards)
    }

    @Test
    fun filter_returnsMatchingCards() {
        // Act
        val result = roundCards.filter { it.type == RoundCardType.BATTLE }

        // Assert
        assertEquals(listOf(sourceCards[0], sourceCards[2]), result.cards)
    }

    @Test
    fun getOrNull_whenIndexValid_returnsCard() {
        // Act
        val result = roundCards.getOrNull(0)

        // Assert
        assertEquals(sourceCards[0], result)
    }

    @Test
    fun getOrNull_whenIndexInvalid_returnsNull() {
        // Assert
        assertNull(roundCards.getOrNull(-1))
        assertNull(roundCards.getOrNull(sourceCards.size))
    }

    @Test
    fun shuffled_returnsCardsInRandomizerOrder() {
        // Act
        val result = roundCards.shuffled(ReversingRandomizer())

        // Assert
        assertEquals(sourceCards.reversed(), result.cards)
    }

    private fun roundCard(name: String, type: RoundCardType): RoundCard =
        RoundCard(
            quantity = 1,
            name = name,
            type = type,
            firstEffect = roundCardEffect("First effect"),
            secondEffect = roundCardEffect("Second effect"),
            backImage = "back"
        )

    private fun roundCardEffect(title: String): RoundCardEffect =
        RoundCardEffect(
            title = title,
            backgroundColor = "background",
            textColor = "text",
            image = "image",
            icon = null,
            effect = GameEffect.UNKNOWN
        )

    private class ReversingRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list.reversed()
    }
}
