package dugsolutions.leaf.v30.cards

import dugsolutions.leaf.v30.cards.di.GameCardsFactory
import dugsolutions.leaf.v30.cards.domain.CardType
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.cards.domain.GameCardID
import dugsolutions.leaf.v30.common.Commons
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameCardManagerTest {

    companion object {
        private const val NONEXISTENT_CARD_ID: GameCardID = 999_999
    }

    private lateinit var cardRegistry: GameCardRegistry
    private lateinit var sourceCards: List<GameCard>
    private lateinit var SUT: GameCardManager

    @BeforeEach
    fun setup() {
        cardRegistry = GameCardRegistry()
        cardRegistry.loadFromCsv(Commons.CARD_LIST)
        sourceCards = cardRegistry.getAllCards()

        SUT = GameCardManager(GameCardsFactory())
        SUT.loadCards(sourceCards)
    }

    @Test
    fun loadCards_withRegistry_loadsCards() {
        // Arrange
        val manager = GameCardManager(GameCardsFactory())

        // Act
        manager.loadCards(cardRegistry)
        val result = manager.getCard(sourceCards.first().id)

        // Assert
        assertEquals(sourceCards.first(), result)
    }

    @Test
    fun getCard_byId_whenCardExists_returnsCard() {
        // Arrange
        val expected = sourceCards.first()

        // Act
        val result = SUT.getCard(expected.id)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getCard_byId_whenCardDoesNotExist_returnsNull() {
        // Act
        val result = SUT.getCard(NONEXISTENT_CARD_ID)

        // Assert
        assertNull(result)
    }

    @Test
    fun getCard_byName_whenCardExists_returnsCard() {
        // Arrange
        val expected = sourceCards.first()

        // Act
        val result = SUT.getCard(expected.name)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getCard_byName_whenCaseDiffers_returnsCard() {
        // Arrange
        val expected = sourceCards.first()

        // Act
        val result = SUT.getCard(expected.name.lowercase())

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getCard_byName_whenCardDoesNotExist_returnsNull() {
        // Act
        val result = SUT.getCard("Nonexistent Card")

        // Assert
        assertNull(result)
    }

    @Test
    fun getCardsByIds_whenAllCardsExist_returnsAllCards() {
        // Arrange
        val expected = sourceCards.take(2)
        val cardIds = expected.map { it.id }

        // Act
        val result = SUT.getCardsByIds(cardIds)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getCardsByIds_whenSomeCardsDoNotExist_returnsOnlyExistingCards() {
        // Arrange
        val expected = listOf(sourceCards[0], sourceCards[2])
        val cardIds = listOf(sourceCards[0].id, NONEXISTENT_CARD_ID, sourceCards[2].id)

        // Act
        val result = SUT.getCardsByIds(cardIds)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getGameCardsByType_whenTypeExists_returnsGameCards() {
        // Arrange
        val expectedCards = sourceCards.filter { it.type == CardType.ROOT }

        // Act
        val result = SUT.getGameCardsByType(CardType.ROOT)

        // Assert
        assertEquals(expectedCards, result.cards)
    }

    @Test
    fun getGameCardsByType_whenManagerEmpty_returnsEmptyGameCards() {
        // Arrange
        val manager = GameCardManager(GameCardsFactory())

        // Act
        val result = manager.getGameCardsByType(CardType.ROOT)

        // Assert
        assertEquals(emptyList(), result.cards)
    }

    @Test
    fun getCardsByType_whenTypeExists_returnsCards() {
        // Arrange
        val expectedCards = sourceCards.filter { it.type == CardType.FLOWER }

        // Act
        val result = SUT.getCardsByType(CardType.FLOWER)

        // Assert
        assertEquals(expectedCards, result)
    }

    @Test
    fun getCardsByType_whenManagerEmpty_returnsEmptyList() {
        // Arrange
        val manager = GameCardManager(GameCardsFactory())

        // Act
        val result = manager.getCardsByType(CardType.ROOT)

        // Assert
        assertEquals(emptyList(), result)
    }

}
