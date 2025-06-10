package dugsolutions.leaf.grove

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.GameCardIDs
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.cards.di.GameCardIDsFactory
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.grove.local.GameCardsUseCase
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.domain.MarketDiceConfig
import dugsolutions.leaf.grove.domain.MarketStackConfig
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.grove.domain.GroveStacks
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.Randomizer
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
        dieFactory = DieFactory(randomizer)
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
        }
        
        val mockGameCards = mockk<GameCards>()
        every { mockGameCardsUseCase(any()) } returns mockGameCards
        
        val mockConfig = mockk<MarketConfig> {
            every { stacks } returns listOf(mockMarketCardConfig)
            every { dice } returns emptyList()
        }
        
        // Act
        SUT.setup(mockConfig)
        
        // Assert
        verify { mockGroveStacks.add(MarketStackID.ROOT_1, mockGameCards) }
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
        }
        
        // Act
        SUT.setup(mockConfig)
        
        // Assert
        verify { mockGroveStacks.addDie( DieSides.D6.value, 4) }
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
    fun addDie_delegatesToMarketStacks() {
        // Arrange
        val die = dieFactory(DieSides.D8)
        every { mockGroveStacks.addDie(die.sides) } returns true

        // Act
        SUT.addDie(die)

        // Assert
        verify { mockGroveStacks.addDie(die.sides) }
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

    @Test
    fun setup_whenStackConfigWithCards_shufflesStack() {
        // Arrange
        val mockMarketCardConfig = mockk<MarketStackConfig> {
            every { which } returns MarketStackID.ROOT_1
            every { cards } returns listOf(mockk())
        }
        
        val mockConfig = mockk<MarketConfig> {
            every { stacks } returns listOf(mockMarketCardConfig)
            every { dice } returns emptyList()
        }
        
        // Act
        SUT.setup(mockConfig)
        
        // Assert
        verify { mockGroveStacks.shuffle(MarketStackID.ROOT_1) }
    }

    @Test
    fun getDiceQuantity_delegatesToMarketStacks() {
        // Arrange
        val sides = 6
        val expectedQuantity = 3
        every { mockGroveStacks.getDiceQuantity(sides) } returns expectedQuantity

        // Act
        val result = SUT.getDiceQuantity(sides)

        // Assert
        assertEquals(expectedQuantity, result)
        verify { mockGroveStacks.getDiceQuantity(sides) }
    }

    @Test
    fun readyForBattlePhase_whenEnoughStacksExhausted_returnsTrue() {
        // Arrange
        MarketStackID.entries.take(Commons.EXHAUSTED_STACK_COUNT).forEach { stackId ->
            every { mockGroveStacks[stackId] } returns mockk {
                every { isEmpty() } returns true
            }
        }
        MarketStackID.entries.drop(Commons.EXHAUSTED_STACK_COUNT).forEach { stackId ->
            every { mockGroveStacks[stackId] } returns mockk {
                every { isEmpty() } returns false
            }
        }

        // Act & Assert
        assertTrue(SUT.readyForBattlePhase)
    }

    @Test
    fun readyForBattlePhase_whenNotEnoughStacksExhausted_returnsFalse() {
        // Arrange
        MarketStackID.entries.take(Commons.EXHAUSTED_STACK_COUNT - 1).forEach { stackId ->
            every { mockGroveStacks[stackId] } returns mockk {
                every { isEmpty() } returns true
            }
        }
        MarketStackID.entries.drop(Commons.EXHAUSTED_STACK_COUNT - 1).forEach { stackId ->
            every { mockGroveStacks[stackId] } returns mockk {
                every { isEmpty() } returns false
            }
        }

        // Act & Assert
        assertFalse(SUT.readyForBattlePhase)
    }

} 
