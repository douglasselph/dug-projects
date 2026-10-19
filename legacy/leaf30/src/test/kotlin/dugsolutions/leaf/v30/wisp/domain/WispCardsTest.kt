package dugsolutions.leaf.v30.wisp.domain

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WispCardsTest {

    private lateinit var sourceCards: List<WispCard>
    private lateinit var wispCards: WispCards

    @BeforeEach
    fun setup() {
        val registry = WispCardRegistry()
        registry.loadFromCsv(Commons.WISP_LIST)
        sourceCards = registry.getAllCards()
        wispCards = WispCards(sourceCards)
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCards() {
        // Arrange
        val mutableCards = sourceCards.take(2).toMutableList()
        val result = WispCards(mutableCards)

        // Act
        mutableCards.clear()

        // Assert
        assertEquals(sourceCards.take(2), result.cards)
    }

    @Test
    fun size_returnsCorrectNumberOfCards() {
        assertEquals(sourceCards.size, wispCards.size)
    }

    @Test
    fun iterator_returnsCardsInOrder() {
        assertEquals(sourceCards, wispCards.toList())
    }

    @Test
    fun get_whenIndexValid_returnsCard() {
        assertEquals(sourceCards[0], wispCards[0])
    }

    @Test
    fun take_returnsFirstNCards() {
        assertEquals(sourceCards.take(3), wispCards.take(3).cards)
    }

    @Test
    fun plus_combinesTwoWispCards() {
        // Arrange
        val first = WispCards(sourceCards.take(2))
        val second = WispCards(sourceCards.drop(2).take(3))

        // Act
        val result = first + second

        // Assert
        assertEquals(sourceCards.take(5), result.cards)
    }

    @Test
    fun filter_returnsMatchingCards() {
        // Arrange
        val expected = sourceCards.filter { it.count == 3 }

        // Act
        val result = wispCards.filter { it.count == 3 }

        // Assert
        assertEquals(expected, result.cards)
    }

    @Test
    fun getOrNull_whenIndexInvalid_returnsNull() {
        assertEquals(null, wispCards.getOrNull(10000))
    }

    @Test
    fun cardIds_returnsCardIdsInOrder() {
        assertEquals(sourceCards.map { it.id }, wispCards.cardIds)
    }

    @Test
    fun shuffled_returnsShuffledCards() {
        // Arrange
        val randomizer = ReversingRandomizer()

        // Act
        val result = wispCards.shuffled(randomizer)

        // Assert
        assertEquals(sourceCards.reversed(), result.cards)
    }

    private class ReversingRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list.reversed()
    }
}
