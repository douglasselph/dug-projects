package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.random.RandomizerTD
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CardManagerTest {

    companion object {
        // Card IDs (as integers since CardID is a typealias for Int)
        private const val NONEXISTENT_CARD_ID = 999
    }

    private lateinit var gameCardsFactory: GameCardsFactory
    private lateinit var randomizer: Randomizer
    private lateinit var SUT: CardManager

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        randomizer = RandomizerTD()
        gameCardsFactory = GameCardsFactory(randomizer = randomizer)

        SUT = CardManager(gameCardsFactory)
        SUT.loadCards(FakeCards.ALL_CARDS)
    }

    @Test
    fun getCard_byId_whenCardExists_returnsCard() {
        // Arrange - Card exists in registry
        
        // Act
        val result = SUT.getCard(FakeCards.sunlightCard.id)
        
        // Assert
        assertEquals(FakeCards.sunlightCard, result)
    }

    @Test
    fun getCard_byId_whenCardDoesNotExist_returnsNull() {
        // Arrange - Card doesn't exist in registry
        
        // Act
        val result = SUT.getCard(NONEXISTENT_CARD_ID)
        
        // Assert
        assertNull(result)
    }

    @Test
    fun getCard_byName_whenCardExists_returnsCard() {
        // Arrange - Card exists in registry
        
        // Act
        val result = SUT.getCard(FakeCards.sunlightCard.name)
        
        // Assert
        assertEquals(FakeCards.sunlightCard, result)
    }

    @Test
    fun getCard_byName_whenCardDoesNotExist_returnsNull() {
        // Arrange - Card doesn't exist in registry
        
        // Act
        val result = SUT.getCard("Nonexistent Card")
        
        // Assert
        assertNull(result)
    }

    @Test
    fun getCardsByIds_whenAllCardsExist_returnsAllCards() {
        // Arrange
        val cardIds = listOf(FakeCards.sunlightCard.id, FakeCards.waterCard.id)
        
        // Act
        val result = SUT.getCardsByIds(cardIds)
        
        // Assert
        assertEquals(2, result.size)
        assertEquals(FakeCards.sunlightCard, result[0])
        assertEquals(FakeCards.waterCard, result[1])
    }

    @Test
    fun getCardsByIds_whenSomeCardsDontExist_returnsOnlyExistingCards() {
        // Arrange
        val cardIds = listOf(FakeCards.sunlightCard.id, NONEXISTENT_CARD_ID, FakeCards.waterCard.id)
        
        // Act
        val result = SUT.getCardsByIds(cardIds)
        
        // Assert
        assertEquals(2, result.size)
        assertEquals(FakeCards.sunlightCard, result[0])
        assertEquals(FakeCards.waterCard, result[1])
    }

    @Test
    fun getGameCardsByType_whenTypeExists_returnsGameCards() {
        // Arrange
        val expectedCards = FakeCards.ALL_RESOURCE // All resource cards
        
        // Act
        val result = SUT.getGameCardsByType(FlourishType.RESOURCE)
        
        // Assert
        assertEquals(expectedCards, result.cards)
    }

    @Test
    fun getGameCardsByType_whenTypeDoesNotExist_returnsEmptyGameCards() {
        // Arrange
        val expectedCards = emptyList<GameCard>()
        
        // Act
        val result = SUT.getGameCardsByType(FlourishType.CANOPY)
        
        // Assert
        assertEquals(expectedCards, result.cards)
    }

    @Test
    fun getCardsByType_whenTypeExists_returnsCards() {
        // Arrange
        val expectedCards = FakeCards.ALL_RESOURCE // All resource cards
        
        // Act
        val result = SUT.getCardsByType(FlourishType.RESOURCE)
        
        // Assert
        assertEquals(expectedCards, result)
    }

    @Test
    fun getCardsByType_whenTypeDoesNotExist_returnsEmptyList() {
        // Arrange
        val expectedCards = emptyList<GameCard>()
        
        // Act
        val result = SUT.getCardsByType(FlourishType.CANOPY)
        
        // Assert
        assertEquals(expectedCards, result)
    }

    @Test
    fun getCard_whenCardHasAddToDieEffect_returnsCardWithCorrectEffect() {
        // Arrange
        val sunlightCard = FakeCards.sunlightCard
        
        // Act
        val result = SUT.getCard(sunlightCard.id)
        
        // Assert
        assertEquals(sunlightCard, result)
        assertEquals(CardEffect.ADD_TO_DIE, result?.primaryEffect)
        assertEquals(2, result?.primaryValue)
    }

    @Test
    fun getCard_whenCardHasRerollAccept2ndEffect_returnsCardWithCorrectEffect() {
        // Arrange
        val waterCard = FakeCards.waterCard
        
        // Act
        val result = SUT.getCard(waterCard.id)
        
        // Assert
        assertEquals(waterCard, result)
        assertEquals(CardEffect.REROLL_ACCEPT_2ND, result?.primaryEffect)
        assertEquals(1, result?.primaryValue)
    }

    @Test
    fun getCard_whenCardHasUpgradeEffect_returnsCardWithCorrectEffect() {
        // Arrange
        val compostCard = FakeCards.compostCard
        
        // Act
        val result = SUT.getCard(compostCard.id)
        
        // Assert
        assertEquals(compostCard, result)
        assertEquals(CardEffect.UPGRADE, result?.primaryEffect)
        assertEquals(1, result?.primaryValue)
        assertEquals("Only if die exists", result?.notes)
    }

    @Test
    fun getCard_whenCardHasMulchEffect_returnsCardWithCorrectEffect() {
        // Arrange
        val mulchCard = FakeCards.mulchCard
        
        // Act
        val result = SUT.getCard(mulchCard.id)
        
        // Assert
        assertEquals(mulchCard, result)
        assertEquals(CardEffect.GRAFT_DIE, result?.primaryEffect)
        assertEquals(1, result?.primaryValue)
    }

    @Test
    fun getCardsByType_whenResourceType_returnsCardsWithVariousEffects() {
        // Arrange
        val expectedCards = FakeCards.ALL_RESOURCE
        
        // Act
        val result = SUT.getCardsByType(FlourishType.RESOURCE)
        
        // Assert
        assertEquals(expectedCards.size, result.size)
        
        // Verify each card has the correct effect
        val sunlightCard = result.find { it.name == "Sunlight" }
        assertEquals(CardEffect.ADD_TO_DIE, sunlightCard?.primaryEffect)
        assertEquals(2, sunlightCard?.primaryValue)
        
        val waterCard = result.find { it.name == "Water" }
        assertEquals(CardEffect.REROLL_ACCEPT_2ND, waterCard?.primaryEffect)
        assertEquals(1, waterCard?.primaryValue)
        
        val compostCard = result.find { it.name == "Compost" }
        assertEquals(CardEffect.UPGRADE, compostCard?.primaryEffect)
        assertEquals(1, compostCard?.primaryValue)
        assertEquals("Only if die exists", compostCard?.notes)
        
        val mulchCard = result.find { it.name == "Mulch" }
        assertEquals(CardEffect.GRAFT_DIE, mulchCard?.primaryEffect)
        assertEquals(1, mulchCard?.primaryValue)
    }
}
