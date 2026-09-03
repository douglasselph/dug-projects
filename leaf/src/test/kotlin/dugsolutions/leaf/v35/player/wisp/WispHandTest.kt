package dugsolutions.leaf.v35.player.wisp

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WispHandTest {

    private lateinit var sourceCards: List<WispCard>

    @BeforeEach
    fun setup() {
        val registry = WispCardRegistry(GameEffectConverter())
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))
        sourceCards = registry.getAllCards()
    }

    @Test
    fun newHand_isEmpty() {
        val hand = WispHand()

        assertEquals(0, hand.size)
        assertTrue(hand.isEmpty)
        assertFalse(hand.isNotEmpty)
        assertEquals(emptyList(), hand.cards.cards)
    }

    @Test
    fun constructor_preservesIncomingOrder() {
        val incoming = sourceCards.take(3)

        val hand = WispHand(incoming)

        assertEquals(3, hand.size)
        assertEquals(incoming, hand.cards.cards)
    }

    @Test
    fun constructor_defensivelyCopiesIncomingList() {
        val incoming = sourceCards.take(2).toMutableList()
        val expected = incoming.toList()

        val hand = WispHand(incoming)
        incoming.clear()

        assertEquals(expected, hand.cards.cards)
    }

    @Test
    fun add_addsCardAndReturnsSameHand() {
        val hand = WispHand()
        val card = sourceCards[0]

        val result = hand.add(card)

        assertTrue(result === hand)
        assertEquals(listOf(card), hand.cards.cards)
        assertEquals(1, hand.size)
        assertFalse(hand.isEmpty)
        assertTrue(hand.isNotEmpty)
    }

    @Test
    fun add_withMultipleCards_preservesOrder() {
        val hand = WispHand()
        val first = sourceCards[0]
        val second = sourceCards[1]

        hand.add(first)
        hand.add(second)

        assertEquals(
            listOf(first, second),
            hand.cards.cards
        )
    }

    @Test
    fun add_allowsMultipleCopiesOfSameWispDefinition() {
        val hand = WispHand()
        val card = sourceCards[0]

        hand.add(card)
        hand.add(card)

        assertEquals(
            listOf(card, card),
            hand.cards.cards
        )
        assertEquals(2, hand.size)
    }

    @Test
    fun addAll_addsEveryCardAndReturnsSameHand() {
        val hand = WispHand()
        val incoming = sourceCards.take(4)

        val result = hand.addAll(incoming)

        assertTrue(result === hand)
        assertEquals(incoming, hand.cards.cards)
        assertEquals(4, hand.size)
    }

    @Test
    fun remove_whenCardExists_removesFirstMatch() {
        val first = sourceCards[0]
        val second = sourceCards[1]
        val hand = WispHand(
            listOf(first, second, first)
        )

        val result = hand.remove(first)

        assertTrue(result)
        assertEquals(
            listOf(second, first),
            hand.cards.cards
        )
    }

    @Test
    fun remove_whenCardDoesNotExist_returnsFalseAndLeavesHandUnchanged() {
        val first = sourceCards[0]
        val missing = sourceCards[1]
        val hand = WispHand(listOf(first))

        val result = hand.remove(missing)

        assertFalse(result)
        assertEquals(
            listOf(first),
            hand.cards.cards
        )
    }

    @Test
    fun cards_returnsDefensiveSnapshot() {
        val hand = WispHand()
        val first = sourceCards[0]
        val second = sourceCards[1]

        hand.add(first)
        val snapshot = hand.cards

        hand.add(second)

        assertEquals(
            listOf(first),
            snapshot.cards
        )
        assertEquals(
            listOf(first, second),
            hand.cards.cards
        )
    }

    @Test
    fun iterator_returnsCardsInHeldOrder() {
        val incoming = sourceCards.take(3)
        val hand = WispHand(incoming)

        assertEquals(
            incoming,
            hand.toList()
        )
    }

    @Test
    fun clear_removesAllCards() {
        val hand = WispHand(sourceCards.take(3))

        hand.clear()

        assertEquals(0, hand.size)
        assertTrue(hand.isEmpty)
        assertFalse(hand.isNotEmpty)
        assertEquals(emptyList(), hand.cards.cards)
    }

    @Test
    fun hand_doesNotImposePermanentFiveCardLimit() {
        val sixCards = List(6) { index ->
            sourceCards[index % sourceCards.size]
        }

        val hand = WispHand(sixCards)

        assertEquals(6, hand.size)
        assertEquals(sixCards, hand.cards.cards)
    }

    private fun dataPath(fileName: String): String =
        Path.of("data", "v35", fileName).toString()
}
