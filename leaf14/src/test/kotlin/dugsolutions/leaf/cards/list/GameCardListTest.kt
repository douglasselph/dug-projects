package dugsolutions.leaf.cards.list

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GameCardListTest {

    companion object {
        private const val EXPECTED_FLOURISH_TYPES_COUNT = 6
    }

    @Test
    fun getByType_whenTypeExists_returnsCorrectCards() {
        // Arrange
        val cards = FakeCards.ALL_CARDS
        val expectedResourceCards = FakeCards.ALL_RESOURCE

        // Act
        val result = cards.getByType(FlourishType.RESOURCE)

        // Assert
        assertEquals(expectedResourceCards.size, result.size)
        assertEquals(expectedResourceCards, result)
    }

    @Test
    fun getByType_whenTypeDoesNotExist_returnsEmptyList() {
        // Arrange
        val cards = FakeCards.ALL_CARDS

        // Act
        val result = cards.getByType(FlourishType.NONE)

        // Assert
        assertEquals(0, result.size)
    }

    // TODO: Add more getByType tests as other card types are implemented
    // @Test
    // fun getByType_whenTypeIsCanopy_returnsCanopyCards() {
    //     // Arrange
    //     val cards = FakeCards.ALL_CARDS
    //     val expectedCanopyCards = FakeCards.ALL_CANOPY
    //
    //     // Act
    //     val result = cards.getByType(FlourishType.CANOPY)
    //
    //     // Assert
    //     assertEquals(expectedCanopyCards.size, result.size)
    //     assertEquals(expectedCanopyCards, result)
    // }

    @Test
    fun getByType_whenEmptyList_returnsEmptyList() {
        // Arrange
        val emptyCards = emptyList<GameCard>()

        // Act
        val result = emptyCards.getByType(FlourishType.ROOT)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun getByType_whenSingleCard_returnsCorrectCard() {
        // Arrange
        val singleCard = listOf(FakeCards.sunlightCard)

        // Act
        val result = singleCard.getByType(FlourishType.RESOURCE)

        // Assert
        assertEquals(1, result.size)
        assertEquals(FakeCards.sunlightCard, result[0])
    }

    @Test
    fun getFlourishTypes_whenMultipleTypes_returnsDistinctTypes() {
        // Arrange
        val cards = FakeCards.ALL_CARDS
        val expectedTypes = listOf(FlourishType.RESOURCE)

        // Act
        val result = cards.getFlourishTypes()

        // Assert
        assertEquals(expectedTypes.size, result.size)
        assertEquals(expectedTypes.toSet(), result.toSet())
    }

    @Test
    fun getFlourishTypes_whenEmptyList_returnsEmptyList() {
        // Arrange
        val emptyCards = emptyList<GameCard>()

        // Act
        val result = emptyCards.getFlourishTypes()

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun getFlourishTypes_whenSingleType_returnsSingleType() {
        // Arrange
        val singleTypeCards = listOf(FakeCards.sunlightCard, FakeCards.waterCard)

        // Act
        val result = singleTypeCards.getFlourishTypes()

        // Assert
        assertEquals(1, result.size)
        assertEquals(FlourishType.RESOURCE, result[0])
    }

    @Test
    fun getFlourishTypes_whenDuplicateTypes_returnsDistinctTypes() {
        // Arrange
        val cardsWithDuplicates = listOf(
            FakeCards.sunlightCard,
            FakeCards.waterCard,
            FakeCards.compostCard,
            FakeCards.mulchCard
        )
        val expectedTypes = listOf(FlourishType.RESOURCE)

        // Act
        val result = cardsWithDuplicates.getFlourishTypes()

        // Assert
        assertEquals(expectedTypes.size, result.size)
        assertEquals(expectedTypes.toSet(), result.toSet())
    }

    @Test
    fun getFlourishTypes_whenAllSameType_returnsSingleType() {
        // Arrange
        val sameTypeCards = listOf(
            FakeCards.sunlightCard,
            FakeCards.waterCard
        )

        // Act
        val result = sameTypeCards.getFlourishTypes()

        // Assert
        assertEquals(1, result.size)
        assertEquals(FlourishType.RESOURCE, result[0])
    }

    // TODO: Add more mixed types tests as other card types are implemented
    // @Test
    // fun getFlourishTypes_whenMixedTypes_returnsAllTypes() {
    //     // Arrange
    //     val mixedCards = listOf(
    //         FakeCards.rootCard,
    //         FakeCards.canopyCard,
    //         FakeCards.vineCard,
    //         FakeCards.flowerCard
    //     )
    //     val expectedTypes = listOf(
    //         FlourishType.ROOT,
    //         FlourishType.CANOPY,
    //         FlourishType.Vine,
    //         FlourishType.FLOWER
    //     )
    //
    //     // Act
    //     val result = mixedCards.getFlourishTypes()
    //
    //     // Assert
    //     assertEquals(expectedTypes.size, result.size)
    //     assertEquals(expectedTypes.toSet(), result.toSet())
    // }
}
