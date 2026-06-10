package dugsolutions.leaf.v30.round

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.round.domain.RoundCardID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoundCardManagerTest {

    companion object {
        private const val NONEXISTENT_CARD_ID: RoundCardID = 999_999
    }

    private lateinit var registry: RoundCardRegistry
    private lateinit var sourceCards: List<RoundCard>
    private lateinit var manager: RoundCardManager

    @BeforeEach
    fun setup() {
        registry = RoundCardRegistry()
        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        sourceCards = registry.getAllCards()
        manager = RoundCardManager(RoundCardsFactory())
        manager.loadCards(sourceCards)
    }

    @Test
    fun loadCards_withRegistry_loadsCards() {
        val manager = RoundCardManager(RoundCardsFactory())

        manager.loadCards(registry)

        assertEquals(sourceCards.first(), manager.getCard(sourceCards.first().id))
    }

    @Test
    fun getCard_byId_whenCardExists_returnsCard() {
        val expected = sourceCards.first()

        val result = manager.getCard(expected.id)

        assertEquals(expected, result)
    }

    @Test
    fun getCard_byId_whenCardDoesNotExist_returnsNull() {
        val result = manager.getCard(NONEXISTENT_CARD_ID)

        assertNull(result)
    }

    @Test
    fun getCard_byName_whenCardExists_returnsCard() {
        val expected = sourceCards.first()

        val result = manager.getCard(expected.name)

        assertEquals(expected, result)
    }

    @Test
    fun getCard_byName_whenCaseDiffers_returnsCard() {
        val expected = sourceCards.first()

        val result = manager.getCard(expected.name.lowercase())

        assertEquals(expected, result)
    }

    @Test
    fun getCardsByIds_whenSomeCardsDoNotExist_returnsOnlyExistingCards() {
        val expected = listOf(sourceCards[0], sourceCards[2])
        val ids = listOf(sourceCards[0].id, NONEXISTENT_CARD_ID, sourceCards[2].id)

        val result = manager.getCardsByIds(ids)

        assertEquals(expected, result)
    }

    @Test
    fun getAllCards_returnsRoundCards() {
        val result = manager.getAllCards()

        assertEquals(sourceCards, result.cards)
    }
}
