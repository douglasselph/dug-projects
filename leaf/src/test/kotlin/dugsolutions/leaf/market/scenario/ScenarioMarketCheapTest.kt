package dugsolutions.leaf.market.scenario

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.di.GameCardIDsFactory
import dugsolutions.leaf.di.GameCardsFactory
import dugsolutions.leaf.market.Market
import dugsolutions.leaf.market.domain.MarketConfig
import dugsolutions.leaf.market.domain.MarketStackID
import dugsolutions.leaf.market.domain.MarketStackType
import dugsolutions.leaf.tool.Randomizer
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ScenarioMarketCheapTest {

    // Test constants
    private companion object {
        private const val TWO_PLAYERS = 2
        private const val FOUR_PLAYERS = 4
        private const val STANDARD_STACK_SIZE = 8
        private const val BLOOM_STACK_SIZE_TWO_PLAYERS = 3 // numPlayers + 1
        private const val BLOOM_STACK_SIZE_FOUR_PLAYERS = 5 // numPlayers + 1
    }

    // System under test
    private lateinit var scenarioMarketCheap: ScenarioMarketCheap

    // Dependencies
    private lateinit var market: Market
    private lateinit var cardManager: CardManager
    private lateinit var gameCardsFactory: GameCardsFactory
    private lateinit var gameCardIDsFactory: GameCardIDsFactory
    private lateinit var randomizer: Randomizer

    // Test data
    private lateinit var rootCards: GameCards
    private lateinit var canopyCards: GameCards
    private lateinit var vineCards: GameCards
    private lateinit var bloomCards: GameCards
    private lateinit var costScore: CostScore

    @BeforeEach
    fun setup() {
        // Arrange - Set up dependencies
        randomizer = Randomizer.create()
        cardManager = mockk(relaxed = true)
        costScore = CostScore()
        gameCardsFactory = GameCardsFactory(randomizer, costScore)
        gameCardIDsFactory = GameCardIDsFactory(cardManager, randomizer)
        market = mockk(relaxed = true)

        // Create card collections using FakeCards instead of mocks
        rootCards = gameCardsFactory(FakeCards.ALL_ROOT)
        canopyCards = gameCardsFactory(FakeCards.ALL_CANOPY)
        vineCards = gameCardsFactory(FakeCards.ALL_VINE)
        bloomCards = gameCardsFactory(FakeCards.ALL_BLOOM)

        // Configure cardManager to return fake cards by type
        every { cardManager.getGameCardsByType(FlourishType.ROOT) } returns rootCards
        every { cardManager.getGameCardsByType(FlourishType.CANOPY) } returns canopyCards
        every { cardManager.getGameCardsByType(FlourishType.VINE) } returns vineCards
        every { cardManager.getGameCardsByType(FlourishType.BLOOM) } returns bloomCards

        // Configure market to accept setup calls
        every { market.setup(any()) } just Runs

        // Create scenario
        scenarioMarketCheap = ScenarioMarketCheap(market, cardManager)
    }

    @Test
    fun invoke_whenTwoPlayers_setsUpMarketWithCorrectConfig() {
        // Arrange
        val configSlot = slot<MarketConfig>()
        every { market.setup(capture(configSlot)) } just Runs

        // Act
        scenarioMarketCheap(TWO_PLAYERS)

        // Assert
        val capturedConfig = configSlot.captured
        
        // Verify stacks configuration
        assertEquals(10, capturedConfig.stacks.size)
        
        // Verify ROOT stacks
        val rootStacks = capturedConfig.stacks.filter { it.which.type == MarketStackType.ROOT }
        assertEquals(2, rootStacks.size)
        rootStacks.forEach { stackConfig ->
            assertEquals(1, stackConfig.cards?.size)
            assertEquals(STANDARD_STACK_SIZE, stackConfig.cards?.first()?.count)
        }
        
        // Verify CANOPY stacks
        val canopyStacks = capturedConfig.stacks.filter { it.which.type == MarketStackType.CANOPY }
        assertEquals(2, canopyStacks.size)
        canopyStacks.forEach { stackConfig ->
            assertEquals(1, stackConfig.cards?.size)
            assertEquals(STANDARD_STACK_SIZE, stackConfig.cards?.first()?.count)
        }
        
        // Verify VINE stacks
        val vineStacks = capturedConfig.stacks.filter { it.which.type == MarketStackType.VINE }
        assertEquals(2, vineStacks.size)
        vineStacks.forEach { stackConfig ->
            assertEquals(1, stackConfig.cards?.size)
            assertEquals(STANDARD_STACK_SIZE, stackConfig.cards?.first()?.count)
        }
        
        // Verify JOINT_RCV stack
        val jointStack = capturedConfig.stacks.find { it.which == MarketStackID.JOINT_RCV }
        assertEquals(null, jointStack?.cards)
        assertEquals(true, jointStack?.cards2 != null)
        
        // Verify BLOOM stacks
        val bloomStacks = capturedConfig.stacks.filter { it.which.type == MarketStackType.BLOOM }
        assertEquals(3, bloomStacks.size)
        bloomStacks.forEach { stackConfig ->
            assertEquals(1, stackConfig.cards?.size)
            assertEquals(BLOOM_STACK_SIZE_TWO_PLAYERS, stackConfig.cards?.first()?.count)
        }
        
        // Verify dice configuration
        assertEquals(6, capturedConfig.dice.size)
        capturedConfig.dice.forEach { diceConfig ->
            assertEquals(TWO_PLAYERS, diceConfig.count)
        }
        
        // Verify bonus dice
        assertEquals(TWO_PLAYERS, capturedConfig.bonusDie.size)
    }

    @Test
    fun invoke_whenFourPlayers_setupsMarketWithMoreBonusDice() {
        // Arrange
        val configSlot = slot<MarketConfig>()
        every { market.setup(capture(configSlot)) } just Runs

        // Act
        scenarioMarketCheap(FOUR_PLAYERS)

        // Assert
        val capturedConfig = configSlot.captured
        
        // Verify dice counts match player count
        capturedConfig.dice.forEach { diceConfig ->
            assertEquals(FOUR_PLAYERS, diceConfig.count)
        }
        
        // Verify bloom stack sizes (players + 1)
        val bloomStacks = capturedConfig.stacks.filter { it.which.type == MarketStackType.BLOOM }
        bloomStacks.forEach { stackConfig ->
            assertEquals(BLOOM_STACK_SIZE_FOUR_PLAYERS, stackConfig.cards?.first()?.count)
        }
        
        // Verify bonus dice count
        assertEquals(FOUR_PLAYERS, capturedConfig.bonusDie.size)
    }

    @Test
    fun invoke_whenAnyPlayerCount_selectsCheapestCardsByType() {
        // Arrange
        val configSlot = slot<MarketConfig>()
        every { market.setup(capture(configSlot)) } just Runs

        // Act
        scenarioMarketCheap(TWO_PLAYERS)

        // Assert
        val capturedConfig = configSlot.captured
        
        // Get the root cards that were selected
        val rootCardConfigs = capturedConfig.stacks
            .filter { it.which.type == MarketStackType.ROOT }
            .flatMap { it.cards ?: emptyList() }
            .map { it.card }
        
        // Verify that the cheapest root cards were selected
        val allRootCardsSortedByCost = FakeCards.ALL_ROOT.sortedBy { costScore(it.cost) }
        rootCardConfigs.forEach { selectedCard ->
            // Selected cards should be among the cheapest ones
            assertEquals(true, selectedCard in allRootCardsSortedByCost.take(2))
        }
        
        // Similar checks for canopy and vine cards
        val canopyCardConfigs = capturedConfig.stacks
            .filter { it.which.type == MarketStackType.CANOPY }
            .flatMap { it.cards ?: emptyList() }
            .map { it.card }
            
        val allCanopyCardsSortedByCost = FakeCards.ALL_CANOPY.sortedBy { costScore(it.cost) }
        canopyCardConfigs.forEach { selectedCard ->
            assertEquals(true, selectedCard in allCanopyCardsSortedByCost.take(2))
        }
        
        val vineCardConfigs = capturedConfig.stacks
            .filter { it.which.type == MarketStackType.VINE }
            .flatMap { it.cards ?: emptyList() }
            .map { it.card }
            
        val allVineCardsSortedByCost = FakeCards.ALL_VINE.sortedBy { costScore(it.cost) }
        vineCardConfigs.forEach { selectedCard ->
            assertEquals(true, selectedCard in allVineCardsSortedByCost.take(2))
        }
    }
} 