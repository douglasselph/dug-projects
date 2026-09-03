package dugsolutions.leaf.v35.plant.domain

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.random.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlantCardsTest {

    private lateinit var sourceCards: List<PlantCard>
    private lateinit var plantCards: PlantCards

    @BeforeEach
    fun setup() {
        sourceCards = listOf(
            plantCard("Vine", PlantType.VINE, 3),
            plantCard("Root", PlantType.ROOT, 1),
            plantCard("Flower", PlantType.FLOWER, 2)
        )
        plantCards = PlantCards(sourceCards)
    }

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalCards() {
        // Arrange
        val mutableCards = sourceCards.toMutableList()
        val result = PlantCards(mutableCards)

        // Act
        mutableCards.clear()

        // Assert
        assertEquals(sourceCards, result.cards)
    }

    @Test
    fun size_returnsCorrectNumberOfCards() {
        // Act
        val result = plantCards.size

        // Assert
        assertEquals(sourceCards.size, result)
    }

    @Test
    fun iterator_returnsCardsInOrder() {
        // Act
        val result = plantCards.toList()

        // Assert
        assertEquals(sourceCards, result)
    }

    @Test
    fun get_whenIndexValid_returnsCard() {
        // Act
        val result = plantCards[1]

        // Assert
        assertEquals(sourceCards[1], result)
    }

    @Test
    fun getByType_whenTypeExists_returnsMatchingCards() {
        // Act
        val result = plantCards.getByType(PlantType.ROOT)

        // Assert
        assertEquals(listOf(sourceCards[1]), result)
    }

    @Test
    fun getByType_whenTypeDoesNotExist_returnsEmptyList() {
        // Arrange
        val vines = PlantCards(sourceCards.filter { it.type == PlantType.VINE })

        // Act
        val result = vines.getByType(PlantType.FLOWER)

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun sortByCost_returnsCardsSortedByCost() {
        // Act
        val result = plantCards.sortByCost()

        // Assert
        assertEquals(sourceCards.sortedBy { it.cost }, result.cards)
    }

    @Test
    fun take_returnsFirstNCards() {
        // Act
        val result = plantCards.take(2)

        // Assert
        assertEquals(sourceCards.take(2), result.cards)
    }

    @Test
    fun plus_combinesTwoPlantCardsCollections() {
        // Arrange
        val first = PlantCards(sourceCards.take(1))
        val second = PlantCards(sourceCards.drop(1))

        // Act
        val result = first + second

        // Assert
        assertEquals(sourceCards, result.cards)
    }

    @Test
    fun filter_returnsMatchingCards() {
        // Act
        val result = plantCards.filter { it.cost >= 2 }

        // Assert
        assertEquals(listOf(sourceCards[0], sourceCards[2]), result.cards)
    }

    @Test
    fun getOrNull_whenIndexValid_returnsCard() {
        // Act
        val result = plantCards.getOrNull(0)

        // Assert
        assertEquals(sourceCards[0], result)
    }

    @Test
    fun getOrNull_whenIndexInvalid_returnsNull() {
        // Assert
        assertNull(plantCards.getOrNull(-1))
        assertNull(plantCards.getOrNull(sourceCards.size))
    }

    @Test
    fun shuffled_returnsCardsInRandomizerOrder() {
        // Act
        val result = plantCards.shuffled(ReversingRandomizer())

        // Assert
        assertEquals(sourceCards.reversed(), result.cards)
    }

    private fun plantCard(name: String, type: PlantType, cost: Int): PlantCard =
        PlantCard(
            quantity = 1,
            name = name,
            title = "$name title",
            type = type,
            cost = cost,
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
