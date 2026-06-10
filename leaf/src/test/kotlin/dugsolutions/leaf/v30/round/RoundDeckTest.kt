package dugsolutions.leaf.v30.round

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.round.domain.RoundCardType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoundDeckTest {

    private lateinit var sourceCards: List<RoundCard>
    private lateinit var manager: RoundCardManager

    @BeforeEach
    fun setup() {
        val registry = RoundCardRegistry()
        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        sourceCards = registry.getAllCards()
        manager = RoundCardManager(RoundCardsFactory())
        manager.loadCards(sourceCards)
    }

    @Test
    fun setup_buildsDeckWithCultivationCardsOnTopAndBattleCardsBelow() {
        val deck = RoundDeck(manager, IdentityRandomizer())

        deck.setup(numBattle = 2, numCultivation = 3)

        assertEquals(5, deck.remaining)
        assertEquals(
            listOf(
                RoundCardType.CULTIVATION,
                RoundCardType.CULTIVATION,
                RoundCardType.CULTIVATION,
                RoundCardType.BATTLE,
                RoundCardType.BATTLE
            ),
            deck.cards.cards.map { it.cardType }
        )
    }

    @Test
    fun setup_shufflesBattleAndCultivationGroupsBeforeTakingCards() {
        val deck = RoundDeck(manager, ReversingRandomizer())
        val expectedCultivation = sourceCards.filter { it.cardType == RoundCardType.CULTIVATION }.reversed().take(2)
        val expectedBattle = sourceCards.filter { it.cardType == RoundCardType.BATTLE }.reversed().take(2)

        deck.setup(numBattle = 2, numCultivation = 2)

        assertEquals(expectedCultivation + expectedBattle, deck.cards.cards)
    }

    @Test
    fun pull_returnsCultivationCardsFirstThenBattleCards() {
        val deck = RoundDeck(manager, IdentityRandomizer())
        deck.setup(numBattle = 2, numCultivation = 2)

        val pulledTypes = listOfNotNull(
            deck.pull()?.cardType,
            deck.pull()?.cardType,
            deck.pull()?.cardType,
            deck.pull()?.cardType
        )

        assertEquals(
            listOf(
                RoundCardType.CULTIVATION,
                RoundCardType.CULTIVATION,
                RoundCardType.BATTLE,
                RoundCardType.BATTLE
            ),
            pulledTypes
        )
        assertTrue(deck.isEmpty)
    }

    @Test
    fun pull_whenDeckEmpty_returnsNull() {
        val deck = RoundDeck(manager, IdentityRandomizer())
        deck.setup(numBattle = 0, numCultivation = 0)

        assertNull(deck.pull())
        assertEquals(0, deck.remaining)
        assertTrue(deck.isEmpty)
    }

    @Test
    fun setup_whenRequestedCountsExceedAvailableCards_throwsException() {
        val deck = RoundDeck(manager, IdentityRandomizer())
        val battleCount = sourceCards.count { it.cardType == RoundCardType.BATTLE }
        val cultivationCount = sourceCards.count { it.cardType == RoundCardType.CULTIVATION }

        assertThrows<IllegalArgumentException> {
            deck.setup(numBattle = battleCount + 1, numCultivation = 0)
        }
        assertThrows<IllegalArgumentException> {
            deck.setup(numBattle = 0, numCultivation = cultivationCount + 1)
        }
    }

    @Test
    fun setup_withNegativeCounts_throwsException() {
        val deck = RoundDeck(manager, IdentityRandomizer())

        assertThrows<IllegalArgumentException> {
            deck.setup(numBattle = -1, numCultivation = 0)
        }
        assertThrows<IllegalArgumentException> {
            deck.setup(numBattle = 0, numCultivation = -1)
        }
    }

    @Test
    fun setup_whenUnknownCardTypeIsLoaded_throwsException() {
        val invalidManager = RoundCardManager(RoundCardsFactory())
        invalidManager.loadCards(listOf(sourceCards.first().copy(name = "Unknown_Card")))
        val deck = RoundDeck(invalidManager, IdentityRandomizer())

        assertThrows<IllegalArgumentException> {
            deck.setup(numBattle = 0, numCultivation = 0)
        }
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
