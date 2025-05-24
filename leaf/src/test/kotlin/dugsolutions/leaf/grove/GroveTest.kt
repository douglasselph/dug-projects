package dugsolutions.leaf.grove

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.di.GameCardIDsFactory
import dugsolutions.leaf.di.GameCardsFactory
import dugsolutions.leaf.grove.domain.GameCardsUseCase
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.domain.MarketDiceConfig
import dugsolutions.leaf.grove.domain.MarketStackConfig
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.grove.domain.GroveStacks
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

class GroveTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
    }

    private lateinit var mockGroveStacks: GroveStacks
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

    private lateinit var SUT: Grove

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
        mockGameCard1 = mockk { every { id } returns CARD_ID_1; }
        mockGameCard2 = mockk { every { id } returns CARD_ID_2; }

        mockGroveStacks = mockk(relaxed = true)
        SUT = Grove(mockGroveStacks, mockGameCardsUseCase)

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
        SUT.setup(mockConfig)
        
        // Assert
        verify { mockGroveStacks.clearAll() }
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
        SUT.setup(mockConfig)
        
        // Assert
        verify { mockGroveStacks.add(MarketStackID.ROOT_1, mockGameCards) }
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
        SUT.setup(mockConfig)
        
        // Assert
        verify { mockGroveStacks.add(MarketStackID.JOINT_RCV, mockGameCards) }
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
        SUT.setup(mockConfig)
        
        // Assert
        verify { mockGroveStacks.addDie( DieSides.D6.value, 4) }
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
        SUT.setup(mockConfig)
        
        // Assert
        verify { mockGroveStacks.setBonusDice(bonusDice) }
    }

    @Test
    fun getTopShowingCards_delegatesToMarketStacks() {
        // Arrange
        val expectedCards = listOf(mockGameCard1, mockGameCard2)
        every { mockGroveStacks.getTopShowingCards() } returns expectedCards

        // Act
        val result = SUT.getTopShowingCards()

        // Assert
        assertEquals(expectedCards, result)
        verify { mockGroveStacks.getTopShowingCards() }
    }

    @Test
    fun getAvailableDiceSides_delegatesToMarketStacks() {
        // Arrange
        val expectedSides = listOf(4, 6, 8, 10, 12, 20)
        every { mockGroveStacks.getAvailableDiceSides() } returns expectedSides

        // Act
        val result = SUT.getAvailableDiceSides()

        // Assert
        assertEquals(expectedSides, result)
        verify { mockGroveStacks.getAvailableDiceSides() }
    }

    @Test
    fun removeCard_delegatesToMarketStacks() {
        // Arrange
        val cardId = CARD_ID_1
        every { mockGroveStacks.removeTopShowingCardOf(cardId) } returns true

        // Act
        SUT.removeCard(cardId)

        // Assert
        verify { mockGroveStacks.removeTopShowingCardOf(cardId) }
    }

    @Test
    fun removeDie_delegatesToMarketStacks() {
        // Arrange
        val die = dieFactory(DieSides.D6)
        every { mockGroveStacks.removeDie(die.sides) } returns true

        // Act
        SUT.removeDie(die)

        // Assert
        verify { mockGroveStacks.removeDie(die.sides) }
    }
    
    @Test
    fun hasDie_whenDieExists_returnsTrue() {
        // Arrange
        val dieSides = 20
        every { mockGroveStacks.hasDie(dieSides) } returns true
        
        // Act
        val result = SUT.hasDie(dieSides)
        
        // Assert
        assertTrue(result)
        verify { mockGroveStacks.hasDie(dieSides) }
    }
    
    @Test
    fun hasDie_whenDieDoesNotExist_returnsFalse() {
        // Arrange
        val dieSides = 20
        every { mockGroveStacks.hasDie(dieSides) } returns false
        
        // Act
        val result = SUT.hasDie(dieSides)
        
        // Assert
        assertFalse(result)
        verify { mockGroveStacks.hasDie(dieSides) }
    }
    
    @Test
    fun getCardsFor_whenStackExists_returnsGameCardIDs() {
        // Arrange
        val stackId = MarketStackID.FLOWER_1
        val expectedGameCardIDs = mockk<GameCardIDs>()
        every { mockGroveStacks[stackId] } returns expectedGameCardIDs
        
        // Act
        val result = SUT.getCardsFor(stackId)
        
        // Assert
        assertSame(expectedGameCardIDs, result)
        verify { mockGroveStacks[stackId] }
    }
    
    @Test
    fun getCardsFor_whenStackDoesNotExist_returnsNull() {
        // Arrange
        val stackId = MarketStackID.FLOWER_3
        every { mockGroveStacks[stackId] } returns null
        
        // Act
        val result = SUT.getCardsFor(stackId)
        
        // Assert
        assertNull(result)
        verify { mockGroveStacks[stackId] }
    }
} 
