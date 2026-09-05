package dugsolutions.leaf.v35.wisp

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.random.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WispDeckTest {

    private lateinit var manager: WispCardManager
    private lateinit var deck: WispDeck

    @BeforeEach
    fun setup() {
        val registry = WispCardRegistry(GameEffectConverter())
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))

        manager = WispCardManager()
        manager.loadCards(registry)

        deck = WispDeck(
            wispCardManager = manager,
            randomizer = ReversingRandomizer()
        )
    }

    @Test
    fun newDeck_isEmptyBeforeReset() {
        // Assert
        assertTrue(deck.isEmpty)
        assertEquals(0, deck.remaining)
        assertEquals(emptyList(), deck.cards.cards)
    }

    @Test
    fun reset_expandsDefinitionsByQuantity() {
        // Arrange
        val expectedPhysicalCards = manager.getAllCards().cards
            .sumOf { it.quantity }

        // Act
        deck.reset()

        // Assert
        assertEquals(36, expectedPhysicalCards)
        assertEquals(expectedPhysicalCards, deck.remaining)
        assertFalse(deck.isEmpty)
    }

    @Test
    fun reset_usesRandomizerToShuffleExpandedDeck() {
        // Arrange
        val expanded = manager.getAllCards().cards
            .flatMap { card -> List(card.quantity) { card } }
        val expected = expanded.reversed()

        // Act
        deck.reset()

        // Assert
        assertEquals(expected, deck.cards.cards)
    }

    @Test
    fun draw_returnsTopCardAndRemovesItFromDeck() {
        // Arrange
        deck.reset()
        val expected = deck.cards.cards[0]
        val before = deck.remaining

        // Act
        val result = deck.draw()

        // Assert
        assertEquals(expected, result)
        assertEquals(before - 1, deck.remaining)
        assertEquals(expected, manager.getCard(expected.name))
    }

    @Test
    fun draw_untilExhausted_returnsEveryPhysicalCardThenNull() {
        // Arrange
        deck.reset()
        val expectedCount = deck.remaining

        // Act
        val drawn = buildList {
            while (!deck.isEmpty) {
                add(deck.draw())
            }
        }

        // Assert
        assertEquals(expectedCount, drawn.size)
        assertTrue(drawn.all { it != null })
        assertEquals(0, deck.remaining)
        assertTrue(deck.isEmpty)
        assertNull(deck.draw())
    }

    @Test
    fun reset_afterDrawing_restoresFullDeck() {
        // Arrange
        deck.reset()
        repeat(5) { deck.draw() }

        // Act
        deck.reset()

        // Assert
        assertEquals(36, deck.remaining)
    }


    @Test
    fun setupExact_preservesProvidedOrderWithoutShuffling() {
        val honor = requireNotNull(manager.getCard("Wisp_Award_VP"))
        val patient = requireNotNull(manager.getCard("Wisp_Award_VP2"))

        deck.setupExact(listOf(honor, patient))

        assertEquals(listOf(honor, patient), deck.cards.cards)
        assertEquals(honor, deck.draw())
        assertEquals(patient, deck.draw())
    }

    @Test
    fun setupExact_rejectsMoreCopiesThanPhysicalQuantity() {
        val patient = requireNotNull(manager.getCard("Wisp_Award_VP2"))

        assertFailsWith<IllegalArgumentException> {
            deck.setupExact(listOf(patient, patient))
        }
    }

    @Test
    fun exactResetCards_makeResetDeterministicWithoutShuffle() {
        val honor = requireNotNull(manager.getCard("Wisp_Award_VP"))
        val exactDeck = WispDeck(
            wispCardManager = manager,
            randomizer = ThrowingShuffleRandomizer(),
            exactResetCards = listOf(honor)
        )

        exactDeck.reset()

        assertEquals(listOf(honor), exactDeck.cards.cards)
    }

    private fun dataPath(fileName: String): String =
        Path.of("data", "v35", fileName).toString()


    private class ThrowingShuffleRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> =
            error("Exact Wisp setup must not shuffle")
    }

    private class ReversingRandomizer : Randomizer {
        override fun nextBoolean(): Boolean =
            throw UnsupportedOperationException()

        override fun nextInt(from: Int, until: Int): Int =
            throw UnsupportedOperationException()

        override fun nextInt(until: Int): Int =
            throw UnsupportedOperationException()

        override fun <T> randomOrNull(list: List<T>): T? =
            throw UnsupportedOperationException()

        override fun <T> shuffled(list: List<T>): List<T> =
            list.reversed()
    }
}
