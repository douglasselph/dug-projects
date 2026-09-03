package dugsolutions.leaf.v35.wisp.domain

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.random.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WispCardsTest {

    private lateinit var sourceCards: List<WispCard>
    private lateinit var wispCards: WispCards

    @BeforeEach
    fun setup() {
        sourceCards = listOf(
            wispCard("First", 1),
            wispCard("Second", 3),
            wispCard("Third", 3)
        )
        wispCards = WispCards(sourceCards)
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCards() {
        // Arrange
        val mutableCards = sourceCards.toMutableList()
        val result = WispCards(mutableCards)

        // Act
        mutableCards.clear()

        // Assert
        assertEquals(sourceCards, result.cards)
    }

    @Test
    fun size_returnsCorrectNumberOfCards() {
        // Act
        val result = wispCards.size

        // Assert
        assertEquals(sourceCards.size, result)
    }

    @Test
    fun iterator_returnsCardsInOrder() {
        // Act
        val result = wispCards.toList()

        // Assert
        assertEquals(sourceCards, result)
    }

    @Test
    fun get_whenIndexValid_returnsCard() {
        // Act
        val result = wispCards[1]

        // Assert
        assertEquals(sourceCards[1], result)
    }

    @Test
    fun take_returnsFirstNCards() {
        // Act
        val result = wispCards.take(2)

        // Assert
        assertEquals(sourceCards.take(2), result.cards)
    }

    @Test
    fun plus_combinesTwoWispCardsCollections() {
        // Arrange
        val first = WispCards(sourceCards.take(1))
        val second = WispCards(sourceCards.drop(1))

        // Act
        val result = first + second

        // Assert
        assertEquals(sourceCards, result.cards)
    }

    @Test
    fun filter_returnsMatchingCards() {
        // Act
        val result = wispCards.filter { it.count == 3 }

        // Assert
        assertEquals(sourceCards.drop(1), result.cards)
    }

    @Test
    fun getOrNull_whenIndexValid_returnsCard() {
        // Act
        val result = wispCards.getOrNull(0)

        // Assert
        assertEquals(sourceCards[0], result)
    }

    @Test
    fun getOrNull_whenIndexInvalid_returnsNull() {
        // Assert
        assertNull(wispCards.getOrNull(-1))
        assertNull(wispCards.getOrNull(sourceCards.size))
    }

    @Test
    fun shuffled_returnsCardsInRandomizerOrder() {
        // Act
        val result = wispCards.shuffled(ReversingRandomizer())

        // Assert
        assertEquals(sourceCards.reversed(), result.cards)
    }

    private fun wispCard(name: String, count: Int): WispCard =
        WispCard(
            quantity = 1,
            name = name,
            title = "$name title",
            count = count,
            effect = GameEffect.UNKNOWN,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = "backdrop"
        )

    private class ReversingRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list.reversed()
    }
}
