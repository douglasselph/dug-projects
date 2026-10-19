package dugsolutions.leaf.v30.wisp

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import dugsolutions.leaf.v30.wisp.domain.WispCard
import dugsolutions.leaf.v30.wisp.domain.WispCardID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WispCardManagerTest {

    companion object {
        private const val NONEXISTENT_CARD_ID: WispCardID = 999_999
    }

    private lateinit var registry: WispCardRegistry
    private lateinit var sourceCards: List<WispCard>
    private lateinit var manager: WispCardManager

    @BeforeEach
    fun setup() {
        registry = WispCardRegistry()
        registry.loadFromCsv(Commons.WISP_LIST)
        sourceCards = registry.getAllCards()
        manager = WispCardManager(WispCardsFactory())
        manager.loadCards(sourceCards)
    }

    @Test
    fun loadCards_withRegistry_loadsCards() {
        // Arrange
        val manager = WispCardManager(WispCardsFactory())

        // Act
        manager.loadCards(registry)

        // Assert
        assertEquals(sourceCards.first(), manager.getCard(sourceCards.first().id))
    }

    @Test
    fun getCard_byId_whenCardExists_returnsCard() {
        // Arrange
        val expected = sourceCards.first()

        // Act
        val result = manager.getCard(expected.id)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getCard_byId_whenCardDoesNotExist_returnsNull() {
        // Act
        val result = manager.getCard(NONEXISTENT_CARD_ID)

        // Assert
        assertNull(result)
    }

    @Test
    fun getCard_byName_whenCardExists_returnsCard() {
        // Arrange
        val expected = sourceCards.first()

        // Act
        val result = manager.getCard(expected.name)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getCard_byName_whenCaseDiffers_returnsCard() {
        // Arrange
        val expected = sourceCards.first()

        // Act
        val result = manager.getCard(expected.name.lowercase())

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getCardsByIds_whenSomeCardsDoNotExist_returnsOnlyExistingCards() {
        // Arrange
        val expected = listOf(sourceCards[0], sourceCards[2])
        val ids = listOf(sourceCards[0].id, NONEXISTENT_CARD_ID, sourceCards[2].id)

        // Act
        val result = manager.getCardsByIds(ids)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getAllCards_returnsWispCards() {
        // Act
        val result = manager.getAllCards()

        // Assert
        assertEquals(sourceCards, result.cards)
    }
}
