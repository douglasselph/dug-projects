package dugsolutions.leaf.v30.round.domain

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.round.RoundCardRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RoundCardsTest {

    private lateinit var sourceCards: List<RoundCard>
    private lateinit var roundCards: RoundCards

    @BeforeEach
    fun setup() {
        val registry = RoundCardRegistry()
        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        sourceCards = registry.getAllCards()
        roundCards = RoundCards(sourceCards)
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCards() {
        val mutableCards = sourceCards.take(2).toMutableList()
        val result = RoundCards(mutableCards)

        mutableCards.clear()

        assertEquals(sourceCards.take(2), result.cards)
    }

    @Test
    fun size_returnsCorrectNumberOfCards() {
        assertEquals(sourceCards.size, roundCards.size)
    }

    @Test
    fun iterator_returnsCardsInOrder() {
        assertEquals(sourceCards, roundCards.toList())
    }

    @Test
    fun get_whenIndexValid_returnsCard() {
        assertEquals(sourceCards[0], roundCards[0])
    }

    @Test
    fun take_returnsFirstNCards() {
        assertEquals(sourceCards.take(3), roundCards.take(3).cards)
    }

    @Test
    fun plus_combinesTwoRoundCards() {
        val first = RoundCards(sourceCards.take(2))
        val second = RoundCards(sourceCards.drop(2).take(3))

        val result = first + second

        assertEquals(sourceCards.take(5), result.cards)
    }

    @Test
    fun filter_returnsMatchingCards() {
        val expected = sourceCards.filter { it.title == "Battle" }

        val result = roundCards.filter { it.title == "Battle" }

        assertEquals(expected, result.cards)
    }

    @Test
    fun getOrNull_whenIndexInvalid_returnsNull() {
        assertEquals(null, roundCards.getOrNull(10000))
    }

    @Test
    fun cardIds_returnsCardIdsInOrder() {
        assertEquals(sourceCards.map { it.id }, roundCards.cardIds)
    }

    @Test
    fun shuffled_returnsShuffledCards() {
        val randomizer = ReversingRandomizer()

        val result = roundCards.shuffled(randomizer)

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
