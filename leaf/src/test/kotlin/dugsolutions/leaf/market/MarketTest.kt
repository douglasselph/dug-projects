package dugsolutions.leaf.market

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.components.SimpleCost
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.di.GameCardIDsFactory
import dugsolutions.leaf.di.GameCardsFactory
import dugsolutions.leaf.market.domain.GameCardsUseCase
import dugsolutions.leaf.market.domain.MarketConfig
import dugsolutions.leaf.market.domain.MarketDiceConfig
import dugsolutions.leaf.market.domain.MarketStackConfig
import dugsolutions.leaf.market.domain.MarketStackID
import dugsolutions.leaf.market.local.MarketStacks
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.Randomizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MarketTest {
    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
    }

    private lateinit var market: Market
    private lateinit var mockMarketStacks: MarketStacks
    private lateinit var mockCardManager: CardManager
    private lateinit var mockGameCardIDsFactory: GameCardIDsFactory
    private lateinit var mockGameCardsFactory: GameCardsFactory
    private lateinit var mockGameCardsUseCase: GameCardsUseCase
    private lateinit var mockRandomizer: Randomizer
    private lateinit var mockGameCard1: GameCard
    private lateinit var mockGameCard2: GameCard
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var player1: Player
    private lateinit var player2: Player
    private lateinit var mockBloomCard: GameCard
    private lateinit var mockRootCard: GameCard

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)
        mockCardManager = mockk(relaxed = true)
        mockGameCardIDsFactory = mockk(relaxed = true)
        every { mockGameCardIDsFactory(any()) } answers {
            GameCardIDs(mockCardManager, firstArg<List<CardID>>(), randomizer)
        }
        mockRandomizer = mockk(relaxed = true)
        mockGameCardsFactory = mockk(relaxed = true)
        every { mockGameCardsFactory(any()) } answers {
            GameCards(
                firstArg<List<GameCard>>(),
                secondArg<Randomizer>(),
                thirdArg<CostScore>()
            )
        }
        mockGameCardsUseCase = mockk(relaxed = true)

        // Setup mock cards
        mockGameCard1 = mockk { every { id } returns CARD_ID_1; every { cost } returns SimpleCost(1) }
        mockGameCard2 = mockk { every { id } returns CARD_ID_2; every { cost } returns SimpleCost(2) }

        mockMarketStacks = mockk(relaxed = true)
        market = Market(mockMarketStacks, mockGameCardsUseCase)

        // Create mock players
        player1 = mockk(relaxed = true)
        player2 = mockk(relaxed = true)

        // Create mock bloom and root cards
        mockBloomCard = mockk(relaxed = true)
        mockRootCard = mockk(relaxed = true)
    }

    @Test
    fun setup_whenMarketConfigProvided_clearsAllStacks() {
        // Arrange
        val mockConfig = mockk<MarketConfig> {
            every { stacks } returns emptyList()
            every { dice } returns emptyList()
            every { bonusDie } returns emptyList()
        }
        
        // Act
        market.setup(mockConfig)
        
        // Assert
        verify { mockMarketStacks.clearAll() }
    }
    
    @Test
    fun setup_whenStackConfigsWithCards_addsCardsToStacks() {
        // Arrange
        val mockMarketCardConfig = mockk<MarketStackConfig> {
            every { which } returns MarketStackID.ROOT_1
            every { cards } returns listOf(mockk())
            every { cards2 } returns null
        }
        
        val mockGameCards = mockk<GameCards>()
        every { mockGameCardsUseCase(any()) } returns mockGameCards
        
        val mockConfig = mockk<MarketConfig> {
            every { stacks } returns listOf(mockMarketCardConfig)
            every { dice } returns emptyList()
            every { bonusDie } returns emptyList()
        }
        
        // Act
        market.setup(mockConfig)
        
        // Assert
        verify { mockMarketStacks.add(MarketStackID.ROOT_1, mockGameCards) }
    }
    
    @Test
    fun setup_whenStackConfigsWithCards2_addsCardsToStacks() {
        // Arrange
        val mockGameCards = mockk<GameCards>()
        val mockMarketCardConfig = mockk<MarketStackConfig> {
            every { which } returns MarketStackID.JOINT_RCV
            every { cards } returns null
            every { cards2 } returns mockGameCards
        }
        
        val mockConfig = mockk<MarketConfig> {
            every { stacks } returns listOf(mockMarketCardConfig)
            every { dice } returns emptyList()
            every { bonusDie } returns emptyList()
        }
        
        // Act
        market.setup(mockConfig)
        
        // Assert
        verify { mockMarketStacks.add(MarketStackID.JOINT_RCV, mockGameCards) }
    }
    
    @Test
    fun setup_whenDiceConfigsProvided_addsDiceToSupply() {
        // Arrange
        val mockDiceConfig = mockk<MarketDiceConfig> {
            every { sides } returns DieSides.D6
            every { count } returns 4
        }
        
        val mockConfig = mockk<MarketConfig> {
            every { stacks } returns emptyList()
            every { dice } returns listOf(mockDiceConfig)
            every { bonusDie } returns emptyList()
        }
        
        // Act
        market.setup(mockConfig)
        
        // Assert
        verify { mockMarketStacks.addDie( DieSides.D6.value, 4) }
    }
    
    @Test
    fun setup_whenBonusDieProvided_setsBonusDice() {
        // Arrange
        val bonusDice = listOf(DieSides.D20, DieSides.D12)
        
        val mockConfig = mockk<MarketConfig> {
            every { stacks } returns emptyList()
            every { dice } returns emptyList()
            every { bonusDie } returns bonusDice
        }
        
        // Act
        market.setup(mockConfig)
        
        // Assert
        verify { mockMarketStacks.setBonusDice(bonusDice) }
    }

    @Test
    fun getTopShowingCards_delegatesToMarketStacks() {
        // Arrange
        val expectedCards = listOf(mockGameCard1, mockGameCard2)
        every { mockMarketStacks.getTopShowingCards() } returns expectedCards

        // Act
        val result = market.getTopShowingCards()

        // Assert
        assertEquals(expectedCards, result)
        verify { mockMarketStacks.getTopShowingCards() }
    }

    @Test
    fun getAvailableDiceSides_delegatesToMarketStacks() {
        // Arrange
        val expectedSides = listOf(4, 6, 8, 10, 12, 20)
        every { mockMarketStacks.getAvailableDiceSides() } returns expectedSides

        // Act
        val result = market.getAvailableDiceSides()

        // Assert
        assertEquals(expectedSides, result)
        verify { mockMarketStacks.getAvailableDiceSides() }
    }

    @Test
    fun removeCard_delegatesToMarketStacks() {
        // Arrange
        val cardId = CARD_ID_1
        every { mockMarketStacks.removeTopShowingCardOf(cardId) } returns true

        // Act
        market.removeCard(cardId)

        // Assert
        verify { mockMarketStacks.removeTopShowingCardOf(cardId) }
    }

    @Test
    fun removeDie_delegatesToMarketStacks() {
        // Arrange
        val die = dieFactory(DieSides.D6)
        every { mockMarketStacks.removeDie(die.sides) } returns true

        // Act
        market.removeDie(die)

        // Assert
        verify { mockMarketStacks.removeDie(die.sides) }
    }
    
    @Test
    fun hasDie_whenDieExists_returnsTrue() {
        // Arrange
        val dieSides = 20
        every { mockMarketStacks.hasDie(dieSides) } returns true
        
        // Act
        val result = market.hasDie(dieSides)
        
        // Assert
        assertTrue(result)
        verify { mockMarketStacks.hasDie(dieSides) }
    }
    
    @Test
    fun hasDie_whenDieDoesNotExist_returnsFalse() {
        // Arrange
        val dieSides = 20
        every { mockMarketStacks.hasDie(dieSides) } returns false
        
        // Act
        val result = market.hasDie(dieSides)
        
        // Assert
        assertFalse(result)
        verify { mockMarketStacks.hasDie(dieSides) }
    }
    
    @Test
    fun getCardsFor_whenStackExists_returnsGameCardIDs() {
        // Arrange
        val stackId = MarketStackID.BLOOM_1
        val expectedGameCardIDs = mockk<GameCardIDs>()
        every { mockMarketStacks[stackId] } returns expectedGameCardIDs
        
        // Act
        val result = market.getCardsFor(stackId)
        
        // Assert
        assertSame(expectedGameCardIDs, result)
        verify { mockMarketStacks[stackId] }
    }
    
    @Test
    fun getCardsFor_whenStackDoesNotExist_returnsNull() {
        // Arrange
        val stackId = MarketStackID.BLOOM_3
        every { mockMarketStacks[stackId] } returns null
        
        // Act
        val result = market.getCardsFor(stackId)
        
        // Assert
        assertNull(result)
        verify { mockMarketStacks[stackId] }
    }
} 