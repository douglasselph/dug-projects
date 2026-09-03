package dugsolutions.leaf.v35.plant.domain

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.random.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlantCardsStackTest {

    private lateinit var sourceCards: List<PlantCard>
    private lateinit var stack: PlantCardsStack

    @BeforeEach
    fun setup() {
        sourceCards = listOf(
            plantCard("First"),
            plantCard("Second"),
            plantCard("Third"),
            plantCard("Fourth")
        )
        stack = PlantCardsStack(sourceCards.take(3))
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCards() {
        // Arrange
        val mutableCards = sourceCards.take(2).toMutableList()
        val result = PlantCardsStack(mutableCards)

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
        val empty = PlantCardsStack()

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
    fun all_returnsSnapshot() {
        // Arrange
        val snapshot = stack.all

        // Act
        stack.clear()

        // Assert
        assertEquals(sourceCards.take(3), snapshot.cards)
        assertTrue(stack.isEmpty)
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
        assertNull(stack[stack.size])
    }

    @Test
    fun add_addsCardAndReturnsSameStack() {
        // Act
        val result = stack.add(sourceCards[3])

        // Assert
        assertSame(stack, result)
        assertEquals(sourceCards, stack.all.cards)
    }

    @Test
    fun addAll_addsCardsInOrderAndReturnsSameStack() {
        // Arrange
        val resultStack = PlantCardsStack(sourceCards.take(1))

        // Act
        val result = resultStack.addAll(sourceCards.drop(1).take(2))

        // Assert
        assertSame(resultStack, result)
        assertEquals(sourceCards.take(3), resultStack.all.cards)
    }

    @Test
    fun remove_whenCardExists_removesCardAndReturnsTrue() {
        // Act
        val result = stack.remove(sourceCards[1])

        // Assert
        assertTrue(result)
        assertEquals(listOf(sourceCards[0], sourceCards[2]), stack.all.cards)
    }

    @Test
    fun remove_whenCardDoesNotExist_returnsFalse() {
        // Act
        val result = stack.remove(sourceCards[3])

        // Assert
        assertFalse(result)
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
        val empty = PlantCardsStack()

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
        // Act
        val result = stack.shuffle(ReversingRandomizer())

        // Assert
        assertSame(stack, result)
        assertEquals(sourceCards.take(3).reversed(), stack.all.cards)
    }

    private fun plantCard(name: String): PlantCard =
        PlantCard(
            quantity = 1,
            name = name,
            title = "$name title",
            type = PlantType.ROOT,
            cost = 1,
            lineIcon = null,
            vpIcon = "vp",
            typeIcon = "type",
            fgColor = "foreground",
            textColor = "text",
            fullImage = "full",
            backgroundImage = "background",
            cardBackgroundImage = "card-background",
            effect = GameEffect.UNKNOWN
        )

    private class ReversingRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list.reversed()
    }
}
