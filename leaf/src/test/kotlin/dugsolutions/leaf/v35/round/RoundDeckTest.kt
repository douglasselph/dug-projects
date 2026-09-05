package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoundDeckTest {

    private lateinit var manager: RoundCardManager
    private lateinit var deck: RoundDeck

    @BeforeEach
    fun setup() {
        val registry = RoundCardRegistry(GameEffectConverter())
        registry.loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))

        manager = RoundCardManager()
        manager.loadCards(registry)

        deck = RoundDeck(
            roundCardManager = manager,
            randomizer = ReversingRandomizer()
        )
    }

    @Test
    fun newDeck_isEmptyAndHasNoTopCard() {
        // Assert
        assertTrue(deck.isEmpty)
        assertEquals(0, deck.remaining)
        assertNull(deck.top)
    }

    @Test
    fun setup_createsRequestedNumberOfRounds() {
        // Act
        deck.setup(
            numBattle = 3,
            numCultivation = 9
        )

        // Assert
        assertEquals(12, deck.remaining)
        assertFalse(deck.isEmpty)
        assertNull(deck.top)
    }

    @Test
    fun setup_placesCultivationRoundsBeforeBattleRounds() {
        // Act
        deck.setup(
            numBattle = 3,
            numCultivation = 5
        )

        // Assert
        assertTrue(
            deck.cards.cards.take(5)
                .all { it.type == RoundCardType.CULTIVATION }
        )
        assertTrue(
            deck.cards.cards.drop(5)
                .all { it.type == RoundCardType.BATTLE }
        )
    }

    @Test
    fun setup_expandsRoundDefinitionsByQuantity() {
        // Act
        deck.setup(
            numBattle = 6,
            numCultivation = 12
        )

        // Assert
        assertEquals(18, deck.remaining)
        assertEquals(
            12,
            deck.cards.cards.count { it.type == RoundCardType.CULTIVATION }
        )
        assertEquals(
            6,
            deck.cards.cards.count { it.type == RoundCardType.BATTLE }
        )
    }

    @Test
    fun setup_whenRequestExceedsPhysicalCards_throws() {
        // Act / Assert
        assertFailsWith<IllegalArgumentException> {
            deck.setup(
                numBattle = 7,
                numCultivation = 0
            )
        }

        assertFailsWith<IllegalArgumentException> {
            deck.setup(
                numBattle = 0,
                numCultivation = 13
            )
        }
    }

    @Test
    fun setup_whenCountsAreNegative_throws() {
        // Act / Assert
        assertFailsWith<IllegalArgumentException> {
            deck.setup(
                numBattle = -1,
                numCultivation = 0
            )
        }

        assertFailsWith<IllegalArgumentException> {
            deck.setup(
                numBattle = 0,
                numCultivation = -1
            )
        }
    }

    @Test
    fun next_revealsTopCardAndRemovesItFromDrawPile() {
        // Arrange
        deck.setup(
            numBattle = 2,
            numCultivation = 2
        )
        val expected = deck.cards.cards[0]
        val before = deck.remaining

        // Act
        val result = deck.next()

        // Assert
        assertEquals(expected, result)
        assertEquals(expected, deck.top)
        assertEquals(before - 1, deck.remaining)
    }

    @Test
    fun next_untilExhausted_returnsNullAndClearsTop() {
        // Arrange
        deck.setup(
            numBattle = 1,
            numCultivation = 1
        )

        // Act
        val first = deck.next()
        val second = deck.next()
        val afterLast = deck.next()

        // Assert
        assertEquals(RoundCardType.CULTIVATION, first?.type)
        assertEquals(RoundCardType.BATTLE, second?.type)
        assertNull(afterLast)
        assertNull(deck.top)
        assertTrue(deck.isEmpty)
        assertEquals(0, deck.remaining)
    }

    @Test
    fun setup_afterDrawing_resetsTopAndBuildsNewDeck() {
        // Arrange
        deck.setup(
            numBattle = 2,
            numCultivation = 2
        )
        deck.next()

        // Act
        deck.setup(
            numBattle = 1,
            numCultivation = 3
        )

        // Assert
        assertNull(deck.top)
        assertEquals(4, deck.remaining)
    }


    @Test
    fun setupExact_preservesProvidedOrderWithoutShuffling() {
        val first = requireNotNull(manager.getCard("Resource_Compost_Mulch"))
        val second = requireNotNull(manager.getCard("Battle_Bloom_Burrow"))

        deck.setupExact(listOf(first, second))

        assertEquals(listOf(first, second), deck.cards.cards)
        assertNull(deck.top)
        assertEquals(first, deck.next())
        assertEquals(second, deck.next())
    }

    @Test
    fun setupExact_rejectsMoreCopiesThanPhysicalQuantity() {
        val card = requireNotNull(manager.getCard("Battle_Bloom_Burrow"))

        assertFailsWith<IllegalArgumentException> {
            deck.setupExact(listOf(card, card))
        }
    }

    private fun dataPath(fileName: String): String =
        Path.of("data", "v35", fileName).toString()

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
