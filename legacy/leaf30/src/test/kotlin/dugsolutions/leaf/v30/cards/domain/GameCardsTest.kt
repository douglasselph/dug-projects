package dugsolutions.leaf.v30.cards.domain

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.random.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GameCardsTest {

    private lateinit var sourceCards: List<GameCard>
    private lateinit var gameCards: GameCards

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        sourceCards = registry.getAllCards()
        gameCards = GameCards(sourceCards)
    }

    @Test
    fun size_returnsCorrectNumberOfCards() {
        // Act
        val result = gameCards.size

        // Assert
        assertEquals(sourceCards.size, result)
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCards() {
        // Arrange
        val mutableCards = sourceCards.take(2).toMutableList()
        val result = GameCards(mutableCards)

        // Act
        mutableCards.clear()

        // Assert
        assertEquals(sourceCards.take(2), result.cards)
    }

    @Test
    fun iterator_returnsCardsInOrder() {
        // Act
        val result = gameCards.toList()

        // Assert
        assertEquals(sourceCards, result)
    }

    @Test
    fun get_whenIndexValid_returnsCard() {
        // Act
        val result = gameCards[0]

        // Assert
        assertEquals(sourceCards[0], result)
    }

    @Test
    fun getByType_whenTypeExists_returnsCorrectCards() {
        // Arrange
        val expected = sourceCards.filter { it.type == CardType.ROOT }

        // Act
        val result = gameCards.getByType(CardType.ROOT)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getByType_whenCardsEmpty_returnsEmptyList() {
        // Arrange
        val empty = GameCards(emptyList())

        // Act
        val result = empty.getByType(CardType.ROOT)

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun sortByCost_returnsCardsSortedByCost() {
        // Arrange
        val expected = sourceCards.sortedBy { it.cost }

        // Act
        val result = gameCards.sortByCost()

        // Assert
        assertEquals(expected, result.cards)
    }

    @Test
    fun take_returnsFirstNCards() {
        // Arrange
        val expected = sourceCards.take(3)

        // Act
        val result = gameCards.take(3)

        // Assert
        assertEquals(expected, result.cards)
    }

    @Test
    fun plus_combinesTwoGameCards() {
        // Arrange
        val first = GameCards(sourceCards.take(2))
        val second = GameCards(sourceCards.drop(2).take(3))
        val expected = first.cards + second.cards

        // Act
        val result = first + second

        // Assert
        assertEquals(expected, result.cards)
    }

    @Test
    fun filter_returnsMatchingCards() {
        // Arrange
        val expected = sourceCards.filter { it.type == CardType.FLOWER }

        // Act
        val result = gameCards.filter { it.type == CardType.FLOWER }

        // Assert
        assertEquals(expected, result.cards)
    }

    @Test
    fun getOrNull_whenIndexValid_returnsCard() {
        // Act
        val result = gameCards.getOrNull(0)

        // Assert
        assertEquals(sourceCards[0], result)
    }

    @Test
    fun getOrNull_whenIndexInvalid_returnsNull() {
        // Act
        val result = gameCards.getOrNull(10000)

        // Assert
        assertEquals(null, result)
    }

    @Test
    fun cardIds_returnsCardIdsInOrder() {
        // Arrange
        val expected = sourceCards.map { it.id }

        // Act
        val result = gameCards.cardIds

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun shuffled_returnsShuffledCards() {
        // Arrange
        val randomizer = ReversingRandomizer()
        val expected = sourceCards.reversed()

        // Act
        val result = gameCards.shuffled(randomizer)

        // Assert
        assertEquals(expected, result.cards)
    }

    private class ReversingRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list.reversed()
    }

}
