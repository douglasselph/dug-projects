package dugsolutions.leaf.v30.cards.domain

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.random.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameCardsStackTest {

    private lateinit var sourceCards: List<GameCard>
    private lateinit var stack: GameCardsStack

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        sourceCards = registry.getAllCards()
        stack = GameCardsStack(sourceCards.take(3))
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCards() {
        // Arrange
        val mutableCards = sourceCards.take(2).toMutableList()
        val result = GameCardsStack(mutableCards)

        // Act
        mutableCards.clear()

        // Assert
        assertEquals(sourceCards.take(2), result.all.cards)
    }

    @Test
    fun size_returnsCurrentCardCount() {
        // Assert
        assertEquals(3, stack.size)
    }

    @Test
    fun isEmpty_whenEmpty_returnsTrue() {
        // Arrange
        val empty = GameCardsStack()

        // Assert
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)
    }

    @Test
    fun isNotEmpty_whenPopulated_returnsTrue() {
        // Assert
        assertTrue(stack.isNotEmpty)
        assertFalse(stack.isEmpty)
    }

    @Test
    fun iterator_returnsCardsInOrder() {
        // Act
        val result = stack.toList()

        // Assert
        assertEquals(sourceCards.take(3), result)
    }

    @Test
    fun all_returnsImmutableSnapshot() {
        // Arrange
        val snapshot = stack.all

        // Act
        stack.clear()

        // Assert
        assertEquals(sourceCards.take(3), snapshot.cards)
        assertTrue(stack.isEmpty)
    }

    @Test
    fun cardIds_returnsCardIdsInOrder() {
        // Arrange
        val expected = sourceCards.take(3).map { it.id }

        // Act
        val result = stack.cardIds

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun get_whenIndexValid_returnsCard() {
        // Act
        val result = stack[1]

        // Assert
        assertEquals(sourceCards[1], result)
    }

    @Test
    fun get_whenIndexInvalid_returnsNull() {
        // Assert
        assertNull(stack[-1])
        assertNull(stack[100])
    }

    @Test
    fun add_addsCardAndReturnsSameStack() {
        // Arrange
        val card = sourceCards[3]

        // Act
        val result = stack.add(card)

        // Assert
        assertEquals(stack, result)
        assertEquals(sourceCards.take(4), stack.all.cards)
    }

    @Test
    fun addAll_addsCardsAndReturnsSameStack() {
        // Arrange
        val cards = sourceCards.drop(3).take(2)

        // Act
        val result = stack.addAll(cards)

        // Assert
        assertEquals(stack, result)
        assertEquals(sourceCards.take(5), stack.all.cards)
    }

    @Test
    fun remove_whenCardExists_removesCard() {
        // Act
        val result = stack.remove(sourceCards[1])

        // Assert
        assertTrue(result)
        assertEquals(listOf(sourceCards[0], sourceCards[2]), stack.all.cards)
    }

    @Test
    fun remove_whenCardDoesNotExist_returnsFalse() {
        // Act
        val result = stack.remove(sourceCards[5])

        // Assert
        assertFalse(result)
        assertEquals(sourceCards.take(3), stack.all.cards)
    }

    @Test
    fun removeById_whenCardExists_returnsRemovedCard() {
        // Act
        val result = stack.remove(sourceCards[1].id)

        // Assert
        assertEquals(sourceCards[1], result)
        assertEquals(listOf(sourceCards[0], sourceCards[2]), stack.all.cards)
    }

    @Test
    fun removeById_whenCardDoesNotExist_returnsNull() {
        // Act
        val result = stack.remove(sourceCards[5].id)

        // Assert
        assertNull(result)
        assertEquals(sourceCards.take(3), stack.all.cards)
    }

    @Test
    fun drawTop_whenPopulated_returnsAndRemovesFirstCard() {
        // Act
        val result = stack.drawTop()

        // Assert
        assertEquals(sourceCards[0], result)
        assertEquals(sourceCards.drop(1).take(2), stack.all.cards)
    }

    @Test
    fun drawTop_whenEmpty_returnsNull() {
        // Arrange
        val empty = GameCardsStack()

        // Act
        val result = empty.drawTop()

        // Assert
        assertNull(result)
    }

    @Test
    fun clear_removesAllCards() {
        // Act
        stack.clear()

        // Assert
        assertTrue(stack.isEmpty)
        assertEquals(emptyList(), stack.all.cards)
    }

    @Test
    fun shuffle_reordersCardsAndReturnsSameStack() {
        // Arrange
        val randomizer = ReversingRandomizer()

        // Act
        val result = stack.shuffle(randomizer)

        // Assert
        assertEquals(stack, result)
        assertEquals(sourceCards.take(3).reversed(), stack.all.cards)
    }

    private class ReversingRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list.reversed()
    }

}
