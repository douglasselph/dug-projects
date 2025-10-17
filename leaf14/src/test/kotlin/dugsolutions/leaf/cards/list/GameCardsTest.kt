package dugsolutions.leaf.cards.list

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.Cost
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.Randomizer
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameCardsTest {

    companion object {
        private const val EXPECTED_SIZE = 20
    }

    private lateinit var randomizer: Randomizer
    private lateinit var gameCards: GameCards

    @BeforeEach
    fun setup() {
        // Create mock randomizer
        randomizer = mockk(relaxed = true)

        // Create GameCards instance with FakeCards
        gameCards = GameCards(FakeCards.ALL_CARDS, randomizer)
    }

    @Test
    fun size_returnsCorrectNumberOfCards() {
        // Arrange
        val expectedSize = FakeCards.ALL_CARDS.size
        
        // Act
        val result = gameCards.size
        
        // Assert
        assertEquals(expectedSize, result)
    }

    @Test
    fun getByType_whenTypeExists_returnsCorrectCards() {
        // Arrange
        val expectedSize = FakeCards.ALL_RESOURCE.size
        
        // Act
        val result = gameCards.getByType(FlourishType.RESOURCE)

        // Assert
        assertEquals(expectedSize, result.size)
        assertEquals(FakeCards.sunlightCard, result[0])
        assertEquals(FakeCards.waterCard, result[1])
        assertEquals(FakeCards.compostCard, result[2])
        assertEquals(FakeCards.mulchCard, result[3])
    }

    @Test
    fun getByType_whenTypeDoesNotExist_returnsEmptyList() {
        // Arrange
        // Act
        val result = gameCards.getByType(FlourishType.NONE)

        // Assert
        assertEquals(0, result.size)
    }

    // TODO: Add more getByType tests as other card types are implemented
    // @Test
    // fun getByType_whenTypeIsCanopy_returnsCanopyCards() {
    //     // Arrange
    //     val expectedSize = FakeCards.ALL_CANOPY.size
    //     
    //     // Act
    //     val result = gameCards.getByType(FlourishType.CANOPY)
    //
    //     // Assert
    //     assertEquals(expectedSize, result.size)
    //     assertEquals(FakeCards.canopyCard, result[0])
    //     assertEquals(FakeCards.canopyCard2, result[1])
    // }

    @Test
    fun plus_combinesTwoGameCards() {
        // Arrange
        val additionalCards = GameCards(FakeCards.ALL_RESOURCE, randomizer)
        val expectedSize = gameCards.size + additionalCards.size

        // Act
        val result = gameCards + additionalCards

        // Assert
        assertEquals(expectedSize, result.size)
    }

    @Test
    fun getOrNull_whenIndexValid_returnsCard() {
        // Arrange
        val validIndex = 0
        
        // Act
        val result = gameCards.getOrNull(validIndex)

        // Assert
        assertEquals(FakeCards.ALL_CARDS[validIndex], result)
    }

    @Test
    fun getOrNull_whenIndexInvalid_returnsNull() {
        // Arrange
        val invalidIndex = 10000
        
        // Act
        val result = gameCards.getOrNull(invalidIndex)

        // Assert
        assertNull(result)
    }

    @Test
    fun getOrNull_whenIndexNegative_returnsNull() {
        // Arrange
        val negativeIndex = -1
        
        // Act
        val result = gameCards.getOrNull(negativeIndex)

        // Assert
        assertNull(result)
    }

    @Test
    fun shuffled_returnsShuffledCards() {
        // Arrange
        val shuffledCards = FakeCards.ALL_RESOURCE
        every { randomizer.shuffled(any<List<GameCard>>()) } returns shuffledCards

        // Act
        val result = gameCards.shuffled()

        // Assert
        assertEquals(shuffledCards.size, result.size)
    }

    @Test
    fun sortByCost_whenCalled_returnsSortedCards() {
        // Arrange
        val cardsWithDifferentCosts = listOf(
            createTestCard("Expensive", Cost.Value(10)),
            createTestCard("Cheap", Cost.None),
            createTestCard("Medium", Cost.Value(5))
        )
        val gameCardsWithCosts = GameCards(cardsWithDifferentCosts, randomizer)

        // Act
        val result = gameCardsWithCosts.sortByCost()

        // Assert
        assertEquals(3, result.size)
        assertEquals("Cheap", result[0].name) // Cost.None = 0
        assertEquals("Medium", result[1].name) // Cost.Value(5) = 5
        assertEquals("Expensive", result[2].name) // Cost.Value(10) = 10
    }

    @Test
    fun sortByCost_whenSameCost_returnsSortedByResilience() {
        // Arrange
        val cardsWithSameCost = listOf(
            createTestCard("LowResilience", Cost.Value(5), resilience = 1),
            createTestCard("HighResilience", Cost.Value(5), resilience = 10),
            createTestCard("MediumResilience", Cost.Value(5), resilience = 5)
        )
        val gameCardsWithSameCost = GameCards(cardsWithSameCost, randomizer)

        // Act
        val result = gameCardsWithSameCost.sortByCost()

        // Assert
        assertEquals(3, result.size)
        assertEquals("LowResilience", result[0].name)
        assertEquals("MediumResilience", result[1].name)
        assertEquals("HighResilience", result[2].name)
    }

    @Test
    fun take_whenValidNumber_returnsCorrectNumberOfCards() {
        // Arrange
        val takeCount = 3

        // Act
        val result = gameCards.take(takeCount)

        // Assert
        assertEquals(takeCount, result.size)
        assertEquals(FakeCards.ALL_CARDS.take(takeCount), result.cards)
    }

    @Test
    fun take_whenNumberExceedsSize_returnsAllCards() {
        // Arrange
        val takeCount = 1000

        // Act
        val result = gameCards.take(takeCount)

        // Assert
        assertEquals(gameCards.size, result.size)
        assertEquals(FakeCards.ALL_CARDS, result.cards)
    }

    @Test
    fun take_whenZero_returnsEmptyGameCards() {
        // Arrange
        val takeCount = 0

        // Act
        val result = gameCards.take(takeCount)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun filter_whenPredicateMatches_returnsFilteredCards() {
        // Arrange
        val predicate: (GameCard) -> Boolean = { it.resilience > 5 }

        // Act
        val result = gameCards.filter(predicate)

        // Assert
        assertTrue(result.cards.all { it.resilience > 5 })
        assertTrue(result.cards.size < gameCards.size)
    }

    @Test
    fun filter_whenNoMatches_returnsEmptyGameCards() {
        // Arrange
        val predicate: (GameCard) -> Boolean = { it.resilience > 1000 }

        // Act
        val result = gameCards.filter(predicate)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun filter_whenAllMatch_returnsAllCards() {
        // Arrange
        val predicate: (GameCard) -> Boolean = { it.resilience >= 0 }

        // Act
        val result = gameCards.filter(predicate)

        // Assert
        assertEquals(gameCards.size, result.size)
    }

    @Test
    fun cardIds_returnsCorrectIds() {
        // Arrange
        val expectedIds = FakeCards.ALL_CARDS.map { it.id }

        // Act
        val result = gameCards.cardIds

        // Assert
        assertEquals(expectedIds, result)
    }

    @Test
    fun iterator_whenUsed_returnsAllCards() {
        // Arrange
        val expectedCards = FakeCards.ALL_CARDS

        // Act
        val result = gameCards.toList()

        // Assert
        assertEquals(expectedCards, result)
    }

    @Test
    fun get_whenValidIndex_returnsCard() {
        // Arrange
        val index = 0
        val expectedCard = FakeCards.ALL_CARDS[index]

        // Act
        val result = gameCards[index]

        // Assert
        assertEquals(expectedCard, result)
    }

    @Test
    fun getByType_whenResourceCards_returnsCardsWithCorrectEffects() {
        // Arrange
        val resourceCards = gameCards.getByType(FlourishType.RESOURCE)
        
        // Act & Assert
        assertEquals(4, resourceCards.size)
        
        val sunlightCard = resourceCards.find { it.name == "Sunlight" }
        assertEquals(CardEffect.ADD_TO_DIE, sunlightCard?.primaryEffect)
        assertEquals(2, sunlightCard?.primaryValue)
        
        val waterCard = resourceCards.find { it.name == "Water" }
        assertEquals(CardEffect.REROLL_ACCEPT_2ND, waterCard?.primaryEffect)
        assertEquals(1, waterCard?.primaryValue)
        
        val compostCard = resourceCards.find { it.name == "Compost" }
        assertEquals(CardEffect.UPGRADE, compostCard?.primaryEffect)
        assertEquals(1, compostCard?.primaryValue)
        assertEquals("Only if die exists", compostCard?.notes)
        
        val mulchCard = resourceCards.find { it.name == "Mulch" }
        assertEquals(CardEffect.GRAFT_DIE, mulchCard?.primaryEffect)
        assertEquals(1, mulchCard?.primaryValue)
    }

    @Test
    fun filter_whenFilteringByEffect_returnsCorrectCards() {
        // Arrange
        val addToDiePredicate: (GameCard) -> Boolean = { it.primaryEffect == CardEffect.ADD_TO_DIE }
        
        // Act
        val result = gameCards.filter(addToDiePredicate)
        
        // Assert
        assertEquals(1, result.size)
        assertEquals("Sunlight", result[0].name)
        assertEquals(CardEffect.ADD_TO_DIE, result[0].primaryEffect)
    }

    @Test
    fun filter_whenFilteringByUpgradeEffect_returnsCorrectCards() {
        // Arrange
        val upgradePredicate: (GameCard) -> Boolean = { it.primaryEffect == CardEffect.UPGRADE }
        
        // Act
        val result = gameCards.filter(upgradePredicate)
        
        // Assert
        assertEquals(1, result.size)
        assertEquals("Compost", result[0].name)
        assertEquals(CardEffect.UPGRADE, result[0].primaryEffect)
        assertEquals("Only if die exists", result[0].notes)
    }

    private fun createTestCard(
        name: String,
        cost: Cost,
        resilience: Int = 0
    ): GameCard {
        return GameCard(
            id = 9999,
            name = name,
            type = FlourishType.NONE,
            resilience = resilience,
            cost = cost,
            phase = dugsolutions.leaf.cards.domain.Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = dugsolutions.leaf.cards.domain.MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )
    }
}
