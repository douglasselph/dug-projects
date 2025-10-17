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

    private val mockGroveStacks: GroveStacks = mockk(relaxed = true)
    private val mockCardManager: CardManager = mockk(relaxed = true)
    private val mockGameCardIDsFactory: GameCardIDsFactory = mockk(relaxed = true)
    private val mockGameCardsFactory: GameCardsFactory = mockk(relaxed = true)
    private val mockGameCardsUseCase: GameCardsUseCase = mockk(relaxed = true)
    private lateinit var mockGameCard1: GameCard
    private lateinit var mockGameCard2: GameCard
    private val randomizer: Randomizer = Randomizer.create()
    private val dieFactory: DieFactory = DieFactory(randomizer)

    private val SUT: Grove = Grove(mockGroveStacks, mockGameCardsUseCase)

    @BeforeEach
    fun setup() {
        every { mockGameCardIDsFactory(any()) } answers {
            GameCardIDs(mockCardManager, firstArg<List<CardID>>(), randomizer)
        }
        every { mockGameCardsFactory(any()) } answers {
            GameCards(
                firstArg<List<GameCard>>(),
                secondArg<Randomizer>(),
                thirdArg<CostScore>()
            )
        }
        // Setup mock cards
        mockGameCard1 = mockk { every { id } returns CARD_ID_1; }
        mockGameCard2 = mockk { every { id } returns CARD_ID_2; }
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
        verify { mockGroveStacks.addDie(DieSides.D6.value, 4) }
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

    @Test
    fun repairWild_whenWild1EmptyAndWild2HasMultipleCards_movesCardFromWild2ToWild1() {
        // Arrange
        val mockWild1 = mockk<GameCardIDs>(relaxed = true)
        val mockWild2 = mockk<GameCardIDs>(relaxed = true)
        val cardId = CARD_ID_1

        every { mockGroveStacks[MarketStackID.WILD_1] } returns mockWild1
        every { mockGroveStacks[MarketStackID.WILD_2] } returns mockWild2
        every { mockWild1.isEmpty() } returns true
        every { mockWild2.isEmpty() } returns false
        every { mockWild2.size } returns 3
        every { mockWild2.removeTop() } returns cardId

        // Act
        SUT.repairWild()

        // Assert
        verify { mockWild2.removeTop() }
        verify { mockWild1.add(cardId) }
    }

    @Test
    fun repairWild_whenWild2EmptyAndWild1HasMultipleCards_movesCardFromWild1ToWild2() {
        // Arrange
        val mockWild1 = mockk<GameCardIDs>(relaxed = true)
        val mockWild2 = mockk<GameCardIDs>(relaxed = true)
        val cardId = CARD_ID_2

        every { mockGroveStacks[MarketStackID.WILD_1] } returns mockWild1
        every { mockGroveStacks[MarketStackID.WILD_2] } returns mockWild2
        every { mockWild1.isEmpty() } returns false
        every { mockWild2.isEmpty() } returns true
        every { mockWild1.size } returns 4
        every { mockWild1.removeTop() } returns cardId

        // Act
        SUT.repairWild()

        // Assert
        verify { mockWild1.isEmpty() }
        verify { mockWild2.isEmpty() }
        verify { mockWild1.size }
        verify { mockWild1.removeTop() }
        verify { mockWild2.add(cardId) }
    }

    @Test
    fun repairWild_whenWild1EmptyAndWild2HasOnlyOneCard_doesNotMoveCard() {
        // Arrange
        val mockWild1 = mockk<GameCardIDs>(relaxed = true)
        val mockWild2 = mockk<GameCardIDs>(relaxed = true)

        every { mockGroveStacks[MarketStackID.WILD_1] } returns mockWild1
        every { mockGroveStacks[MarketStackID.WILD_2] } returns mockWild2
        every { mockWild1.isEmpty() } returns true
        every { mockWild2.isEmpty() } returns false
        every { mockWild2.size } returns 1

        // Act
        SUT.repairWild()

        // Assert
        verify { mockWild1.isEmpty() }
        verify { mockWild2.isEmpty() }
        verify { mockWild2.size }
        verify(exactly = 0) { mockWild2.removeTop() }
        verify(exactly = 0) { mockWild1.add(any<CardID>()) }
    }

    @Test
    fun repairWild_whenWild2EmptyAndWild1HasOnlyOneCard_doesNotMoveCard() {
        // Arrange
        val mockWild1 = mockk<GameCardIDs>(relaxed = true)
        val mockWild2 = mockk<GameCardIDs>(relaxed = true)

        every { mockGroveStacks[MarketStackID.WILD_1] } returns mockWild1
        every { mockGroveStacks[MarketStackID.WILD_2] } returns mockWild2
        every { mockWild1.isEmpty() } returns false
        every { mockWild2.isEmpty() } returns true
        every { mockWild1.size } returns 1

        // Act
        SUT.repairWild()

        // Assert
        verify { mockWild1.isEmpty() }
        verify { mockWild2.isEmpty() }
        verify { mockWild1.size }
        verify(exactly = 0) { mockWild1.removeTop() }
        verify(exactly = 0) { mockWild2.add(any<CardID>()) }
    }

    @Test
    fun repairWild_whenBothWildStacksEmpty_doesNothing() {
        // Arrange
        val mockWild1 = mockk<GameCardIDs>(relaxed = true)
        val mockWild2 = mockk<GameCardIDs>(relaxed = true)

        every { mockGroveStacks[MarketStackID.WILD_1] } returns mockWild1
        every { mockGroveStacks[MarketStackID.WILD_2] } returns mockWild2
        every { mockWild1.isEmpty() } returns true
        every { mockWild2.isEmpty() } returns true

        // Act
        SUT.repairWild()

        // Assert
        verify { mockWild1.isEmpty() }
        verify { mockWild2.isEmpty() }
        verify(exactly = 0) { mockWild1.removeTop() }
        verify(exactly = 0) { mockWild2.removeTop() }
        verify(exactly = 0) { mockWild1.add(any<CardID>()) }
        verify(exactly = 0) { mockWild2.add(any<CardID>()) }
    }

    @Test
    fun repairWild_whenBothWildStacksHaveCards_doesNothing() {
        // Arrange
        val mockWild1 = mockk<GameCardIDs>(relaxed = true)
        val mockWild2 = mockk<GameCardIDs>(relaxed = true)

        every { mockGroveStacks[MarketStackID.WILD_1] } returns mockWild1
        every { mockGroveStacks[MarketStackID.WILD_2] } returns mockWild2
        every { mockWild1.isEmpty() } returns false
        every { mockWild2.isEmpty() } returns false

        // Act
        SUT.repairWild()

        // Assert
        verify { mockWild1.isEmpty() }
        verify { mockWild2.isEmpty() }
        verify(exactly = 0) { mockWild1.size }
        verify(exactly = 0) { mockWild2.size }
        verify(exactly = 0) { mockWild1.removeTop() }
        verify(exactly = 0) { mockWild2.removeTop() }
        verify(exactly = 0) { mockWild1.add(any<CardID>()) }
        verify(exactly = 0) { mockWild2.add(any<CardID>()) }
    }

    @Test
    fun repairWild_whenWild1IsNull_returnsEarly() {
        // Arrange
        val mockWild2 = mockk<GameCardIDs>(relaxed = true)

        every { mockGroveStacks[MarketStackID.WILD_1] } returns null
        every { mockGroveStacks[MarketStackID.WILD_2] } returns mockWild2

        // Act
        SUT.repairWild()

        // Assert
        verify { mockGroveStacks[MarketStackID.WILD_1] }
        verify(exactly = 0) { mockGroveStacks[MarketStackID.WILD_2] }
        verify(exactly = 0) { mockWild2.isEmpty() }
    }

    @Test
    fun repairWild_whenWild2IsNull_returnsEarly() {
        // Arrange
        val mockWild1 = mockk<GameCardIDs>(relaxed = true)

        every { mockGroveStacks[MarketStackID.WILD_1] } returns mockWild1
        every { mockGroveStacks[MarketStackID.WILD_2] } returns null

        // Act
        SUT.repairWild()

        // Assert
        verify { mockGroveStacks[MarketStackID.WILD_1] }
        verify { mockGroveStacks[MarketStackID.WILD_2] }
        verify(exactly = 0) { mockWild1.isEmpty() }
    }

    @Test
    fun repairWild_whenWild2RemoveTopReturnsNull_doesNotAddToWild1() {
        // Arrange
        val mockWild1 = mockk<GameCardIDs>(relaxed = true)
        val mockWild2 = mockk<GameCardIDs>(relaxed = true)

        every { mockGroveStacks[MarketStackID.WILD_1] } returns mockWild1
        every { mockGroveStacks[MarketStackID.WILD_2] } returns mockWild2
        every { mockWild1.isEmpty() } returns true
        every { mockWild2.isEmpty() } returns false
        every { mockWild2.size } returns 3
        every { mockWild2.removeTop() } returns null

        // Act
        SUT.repairWild()

        // Assert
        verify { mockWild2.removeTop() }
        verify(exactly = 0) { mockWild1.add(any<CardID>()) }
    }

    @Test
    fun repairWild_whenWild1RemoveTopReturnsNull_doesNotAddToWild2() {
        // Arrange
        val mockWild1 = mockk<GameCardIDs>(relaxed = true)
        val mockWild2 = mockk<GameCardIDs>(relaxed = true)

        every { mockGroveStacks[MarketStackID.WILD_1] } returns mockWild1
        every { mockGroveStacks[MarketStackID.WILD_2] } returns mockWild2
        every { mockWild1.isEmpty() } returns false
        every { mockWild2.isEmpty() } returns true
        every { mockWild1.size } returns 4
        every { mockWild1.removeTop() } returns null

        // Act
        SUT.repairWild()

        // Assert
        verify { mockWild1.isEmpty() }
        verify { mockWild2.isEmpty() }
        verify { mockWild1.size }
        verify { mockWild1.removeTop() }
        verify(exactly = 0) { mockWild2.add(any<CardID>()) }
    }

} 
