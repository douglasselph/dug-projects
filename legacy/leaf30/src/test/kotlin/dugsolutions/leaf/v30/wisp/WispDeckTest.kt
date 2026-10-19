package dugsolutions.leaf.v30.wisp

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import dugsolutions.leaf.v30.wisp.domain.WispCard
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WispDeckTest {

    private lateinit var sourceCards: List<WispCard>
    private lateinit var manager: WispCardManager

    @BeforeEach
    fun setup() {
        val registry = WispCardRegistry()
        registry.loadFromCsv(Commons.WISP_LIST)
        sourceCards = registry.getAllCards()
        manager = WispCardManager(WispCardsFactory())
        manager.loadCards(sourceCards)
    }

    @Test
    fun reset_expandsCardsByQuantity() {
        val deck = WispDeck(manager, IdentityRandomizer())

        deck.reset()

        assertEquals(sourceCards.sumOf { it.quantity }, deck.remaining)
        sourceCards.forEach { card ->
            assertEquals(card.quantity, deck.cards.cards.count { it == card })
        }
        assertFalse(deck.isEmpty)
    }

    @Test
    fun reset_shufflesExpandedCards() {
        val deck = WispDeck(manager, ReversingRandomizer())

        deck.reset()

        assertEquals(sourceCards.last(), deck.cards[0])
    }

    @Test
    fun draw_returnsFirstCardAndRemovesItFromDeck() {
        val deck = WispDeck(manager, IdentityRandomizer())
        deck.reset()

        val result = deck.draw()

        assertEquals(sourceCards.first(), result)
        assertEquals(sourceCards.sumOf { it.quantity } - 1, deck.remaining)
    }

    @Test
    fun draw_untilDeckEmpty_returnsNull() {
        val deck = WispDeck(manager, IdentityRandomizer())
        deck.reset()

        repeat(sourceCards.sumOf { it.quantity }) {
            assertTrue(deck.draw() != null)
        }

        assertNull(deck.draw())
        assertEquals(0, deck.remaining)
        assertTrue(deck.isEmpty)
    }

    @Test
    fun reset_afterDrawing_restoresFullDeck() {
        val deck = WispDeck(manager, IdentityRandomizer())
        deck.reset()
        deck.draw()
        deck.draw()

        deck.reset()

        assertEquals(sourceCards.sumOf { it.quantity }, deck.remaining)
    }

    @Test
    fun reset_whenManagerHasNoCards_createsEmptyDeck() {
        val emptyManager = WispCardManager(WispCardsFactory())
        val deck = WispDeck(emptyManager, IdentityRandomizer())

        deck.reset()

        assertTrue(deck.isEmpty)
        assertNull(deck.draw())
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }

    private class ReversingRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list.reversed()
    }
}
