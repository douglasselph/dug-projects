package dugsolutions.leaf.grove.domain

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
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.Randomizer
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroveStacksTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
        private const val CARD_ID_3 = 3
    }

    private lateinit var mockCardManager: CardManager
    private lateinit var mockGameCardIDsFactory: GameCardIDsFactory
    private lateinit var mockGameCardsFactory: GameCardsFactory
    private lateinit var gameCardIDsFactory: GameCardIDsFactory
    private lateinit var gameCardsFactory: GameCardsFactory
    private lateinit var mockRandomizer: Randomizer
    private lateinit var mockGameCard1: GameCard
    private lateinit var mockGameCard2: GameCard
    private lateinit var mockGameCard3: GameCard
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var player1: Player
    private lateinit var player2: Player
    private lateinit var mockBloomCard: GameCard
    private lateinit var mockRootCard: GameCard
    private lateinit var mockGameCards: GameCards
    private lateinit var mockGameCardIDs: GameCardIDs
    private lateinit var costScore: CostScore

    private lateinit var SUT: GroveStacks
    private lateinit var SUT2: GroveStacks

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)
        mockCardManager = mockk(relaxed = true)
        mockGameCardIDsFactory = mockk(relaxed = true)
        mockGameCardIDs = mockk(relaxed = true)
        mockRandomizer = mockk(relaxed = true)
        mockGameCardsFactory = mockk(relaxed = true)
        mockGameCards = mockk(relaxed = true)
        player1 = mockk(relaxed = true)
        player2 = mockk(relaxed = true)
        mockBloomCard = mockk(relaxed = true)
        mockRootCard = mockk(relaxed = true)
        costScore = mockk(relaxed = true)

        gameCardIDsFactory = GameCardIDsFactory(mockCardManager, randomizer)
        gameCardsFactory = GameCardsFactory(randomizer, costScore)

        every { mockGameCardIDsFactory(any()) } returns mockGameCardIDs
        every { mockGameCardsFactory(any()) } returns mockGameCards

        mockGameCard1 = mockk {
            every { id } returns CARD_ID_1
        }
        mockGameCard2 = mockk {
            every { id } returns CARD_ID_2
        }
        mockGameCard3 = mockk {
            every { id } returns CARD_ID_3
        }

        every { mockGameCards.cards } returns listOf(mockGameCard1, mockGameCard2, mockGameCard3)
        every { mockGameCardIDs.cardIds } returns listOf(CARD_ID_1, CARD_ID_2, CARD_ID_3)
        every { mockCardManager.getCard(CARD_ID_1) } returns mockGameCard1
        every { mockCardManager.getCard(CARD_ID_2) } returns mockGameCard2
        every { mockCardManager.getCard(CARD_ID_3) } returns mockGameCard3

        SUT = GroveStacks(mockCardManager, mockGameCardIDsFactory, dieFactory)
        SUT2 = GroveStacks(mockCardManager, gameCardIDsFactory, dieFactory)
    }

    private fun createGameCards(cards: List<GameCard>): GameCards {
        return mockGameCardsFactory(cards)
    }

    // region Stack Management Tests

    @Test
    fun add_withGameCards_addsCardsToStack() {
        // Arrange
        val cards = gameCardsFactory(listOf(mockGameCard1, mockGameCard2))
        val stack = MarketStackID.ROOT_1

        // Act
        SUT2.add(stack, cards)

        // Assert
        val stackCards = SUT2[stack]?.cardIds
        assertEquals(2, stackCards?.size)
        assertTrue(stackCards?.contains(CARD_ID_1) == true)
        assertTrue(stackCards?.contains(CARD_ID_2) == true)
    }

    @Test
    fun add_withCardIds_addsCardIdsToStack() {
        // Arrange
        val cardIds = gameCardIDsFactory(listOf(CARD_ID_1, CARD_ID_2))
        val stack = MarketStackID.ROOT_1

        // Act
        SUT2.add(stack, cardIds)

        // Assert
        val stackCards = SUT2[stack]?.cardIds
        assertEquals(2, stackCards?.size)
        assertTrue(stackCards?.contains(CARD_ID_1) == true)
        assertTrue(stackCards?.contains(CARD_ID_2) == true)
    }

    @Test
    fun set_withGameCards_replacesStackContents() {
        // Arrange
        val cards = gameCardsFactory(listOf(mockGameCard1, mockGameCard2))
        val stack = MarketStackID.ROOT_1

        // Act
        SUT2.set(stack, cards)

        // Assert
        val stackCards = SUT2[stack]?.cardIds
        assertEquals(2, stackCards?.size)
        assertTrue(stackCards?.contains(CARD_ID_1) == true)
        assertTrue(stackCards?.contains(CARD_ID_2) == true)
    }

    @Test
    fun set_withCardIds_replacesStackContents() {
        // Arrange
        val cardIds = gameCardIDsFactory(listOf(CARD_ID_1, CARD_ID_2))
        val stack = MarketStackID.ROOT_1

        // Act
        SUT2.set(stack, cardIds)

        // Assert
        val stackCards = SUT2[stack]?.cardIds
        assertEquals(2, stackCards?.size)
        assertTrue(stackCards?.contains(CARD_ID_1) == true)
        assertTrue(stackCards?.contains(CARD_ID_2) == true)
    }

    @Test
    fun repeat_addsMultipleCopiesOfCard() {
        // Arrange
        val stack = MarketStackID.ROOT_1
        val count = 3

        // Act
        SUT2.repeat(stack, CARD_ID_1, count)

        // Assert
        val stackCards = SUT2[stack]?.cardIds
        assertEquals(count, stackCards?.size)
        assertTrue(stackCards?.all { it == CARD_ID_1 } == true)
    }

    @Test
    fun clearAll_removesAllContents() {
        // Arrange
        SUT2.add(MarketStackID.ROOT_1, gameCardsFactory(listOf(mockGameCard1)))
        SUT2.add(MarketStackID.FLOWER_1, gameCardsFactory(listOf(mockGameCard2)))
        SUT2.setDiceCount(DieSides.D6, 2)

        // Act
        SUT2.clearAll()

        // Assert
        MarketStackID.entries.forEach { stack ->
            assertTrue(SUT2[stack]?.isEmpty() == true)
        }
        assertEquals(0, SUT2.getAvailableDiceSides().size)
    }

    // endregion Stack Management Tests

    // region Dice Management Tests

    @Test
    fun setBonusDice_addsAllDiceInList() {
        // Arrange
        val diceSides = listOf(DieSides.D20, DieSides.D12, DieSides.D10)
        
        // Act
        SUT.setBonusDice(diceSides)

        // Assert
        assertEquals(3, SUT.numBonusDice)
        assertEquals(20, SUT.useNextBonusDie()?.sides)
        assertEquals(12, SUT.useNextBonusDie()?.sides)
        assertEquals(10, SUT.useNextBonusDie()?.sides)
    }

    @Test
    fun setBonusDice_withEmptyList_doesNothing() {
        // Arrange
        val emptyList = emptyList<DieSides>()
        
        // Act
        SUT.setBonusDice(emptyList)

        // Assert
        assertEquals(0, SUT.numBonusDice)
    }
    
    @Test
    fun setBonusDice_replacesExistingBonusDice() {
        // Arrange
        SUT.setBonusDice(listOf(DieSides.D6, DieSides.D6))
        assertEquals(2, SUT.numBonusDice)
        
        // Act
        SUT.setBonusDice(listOf(DieSides.D20))
        
        // Assert
        assertEquals(1, SUT.numBonusDice)
        assertEquals(20, SUT.useNextBonusDie()?.sides)
    }

    @Test
    fun setDiceCount_setsAllValidDiceToCount() {
        // Arrange
        val dieCount = 2
        
        // Act
        DiceSupply.VALID_DICE_SIDES.forEach { sides ->
            SUT.setDiceCount(DieSides.from(sides), dieCount)
        }

        // Assert
        DiceSupply.VALID_DICE_SIDES.forEach { sides ->
            assertEquals(dieCount, SUT.getDiceQuantity(sides))
        }
    }

    @Test
    fun getAvailableDiceSides_returnsOnlyAvailableSides() {
        // Arrange
        SUT2.setDiceCount(DieSides.D4, 2)
        SUT2.setDiceCount(DieSides.D6, 2)
        SUT2.setDiceCount(DieSides.D8, 2)
        SUT2.setDiceCount(DieSides.D10, 2)
        SUT2.setDiceCount(DieSides.D12, 2)
        SUT2.setDiceCount(DieSides.D20, 2)
        SUT2.removeDie(6)
        SUT2.removeDie(6)

        // Act
        val availableSides = SUT2.getAvailableDiceSides()

        // Assert
        assertTrue(availableSides.containsAll(setOf(4, 8, 10, 12, 20)))
        assertFalse(availableSides.contains(6))
    }

    @Test
    fun removeDie_whenDieExists_returnsTrueAndDecrementsQuantity() {
        // Arrange
        SUT.setDiceCount(DieSides.D6, 2)

        // Act
        val result = SUT.removeDie(6)

        // Assert
        assertTrue(result)
        assertEquals(1, SUT.getDiceQuantity(6))
    }

    @Test
    fun removeDie_whenDieDoesNotExist_returnsFalse() {
        // Act
        val result = SUT.removeDie(6)

        // Assert
        assertFalse(result)
        assertEquals(0, SUT.getDiceQuantity(6))
    }

    @Test
    fun addDie_whenValidDie_returnsTrueAndIncrementsQuantity() {
        // Act
        val result = SUT.addDie(6)

        // Assert
        assertTrue(result)
        assertEquals(1, SUT.getDiceQuantity(6))
    }

    @Test
    fun addDie_whenInvalidDie_returnsFalse() {
        // Act
        val result = SUT.addDie(7)

        // Assert
        assertFalse(result)
        assertEquals(0, SUT.getDiceQuantity(7))
    }

    // endregion Dice Management Tests

    // region HasDie Tests
    
    @Test
    fun hasDie_whenDieExistsWithPositiveQuantity_returnsTrue() {
        // Arrange
        SUT.setDiceCount(DieSides.D6, 1)
        
        // Act
        val result = SUT.hasDie(6)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun hasDie_whenDieExistsWithMultipleQuantity_returnsTrue() {
        // Arrange
        SUT.setDiceCount(DieSides.D8, 5)
        
        // Act
        val result = SUT.hasDie(8)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun hasDie_whenDieExistsWithZeroQuantity_returnsFalse() {
        // Arrange
        SUT.setDiceCount(DieSides.D10, 0)
        
        // Act
        val result = SUT.hasDie(10)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun hasDie_whenDieDoesNotExist_returnsFalse() {
        // Act
        val result = SUT.hasDie(12)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun hasDie_whenInvalidDie_returnsFalse() {
        // Act
        val result = SUT.hasDie(7)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun hasDie_afterAddAndRemove_reflectsCurrentState() {
        // Arrange
        SUT.setDiceCount(DieSides.D20, 1)
        assertTrue(SUT.hasDie(20))
        
        // Act
        SUT.removeDie(20)
        
        // Assert
        assertFalse(SUT.hasDie(20))
    }
    
    // endregion HasDie Tests

    // region Card Query Tests

    @Test
    fun getStacksByType_returnsCombinedCardsOfType() {
        // Arrange
        SUT2.add(MarketStackID.ROOT_1, gameCardsFactory(listOf(mockGameCard1)))
        SUT2.add(MarketStackID.ROOT_2, gameCardsFactory(listOf(mockGameCard2)))
        SUT2.add(MarketStackID.FLOWER_1, gameCardsFactory(listOf(mockGameCard3)))

        // Act
        val rootCards = SUT2.getStacksByType(MarketStackType.ROOT)

        // Assert
        assertEquals(2, rootCards.cardIds.size)
        assertTrue(rootCards.cardIds.contains(CARD_ID_1))
        assertTrue(rootCards.cardIds.contains(CARD_ID_2))
    }

    @Test
    fun findStacksWithTopShowingForCard_returnsCorrectStacks() {
        // Arrange
        SUT2.add(MarketStackID.ROOT_1, gameCardsFactory(listOf(mockGameCard1)))
        SUT2.add(MarketStackID.ROOT_2, gameCardsFactory(listOf(mockGameCard2)))

        // Act
        val stacks = SUT2.findStacksWithTopShowingForCard(CARD_ID_1)

        // Assert
        assertEquals(1, stacks.size)
        assertEquals(MarketStackID.ROOT_1, stacks[0])
    }

    @Test
    fun getTopShowingCards_returnsTopCardsFromAllStacks() {
        // Arrange
        SUT2.add(MarketStackID.ROOT_1, gameCardsFactory(listOf(mockGameCard1)))
        SUT2.add(MarketStackID.FLOWER_1, gameCardsFactory(listOf(mockGameCard2)))

        // Act
        val topCards = SUT2.getTopShowingCards()

        // Assert
        assertEquals(2, topCards.size)
        assertTrue(topCards.contains(mockGameCard1))
        assertTrue(topCards.contains(mockGameCard2))
    }

    @Test
    fun findGameCardByID_returnsCorrectCard() {
        // Arrange
        every { mockCardManager.getCard(CARD_ID_1) } returns mockGameCard1

        // Act
        val card = SUT.findGameCardByID(CARD_ID_1)

        // Assert
        assertEquals(mockGameCard1, card)
    }

    @Test
    fun isCardShowing_whenCardIsTop_returnsTrue() {
        // Arrange
        SUT.add(MarketStackID.ROOT_1, createGameCards(listOf(mockGameCard1)))

        // Act
        val isShowing = SUT.isCardShowing(CARD_ID_1)

        // Assert
        assertTrue(isShowing)
    }

    @Test
    fun isCardShowing_whenCardIsNotTop_returnsFalse() {
        // Arrange
        SUT.add(MarketStackID.ROOT_1, createGameCards(listOf(mockGameCard1)))

        // Act
        val isShowing = SUT.isCardShowing(CARD_ID_2)

        // Assert
        assertFalse(isShowing)
    }

    @Test
    fun removeTopShowingCardOf_whenCardIsTop_returnsTrueAndRemovesCard() {
        // Arrange
        SUT2.add(MarketStackID.ROOT_1, gameCardsFactory(listOf(mockGameCard1)))

        // Act
        val result = SUT2.removeTopShowingCardOf(CARD_ID_1)

        // Assert
        assertTrue(result)
        assertFalse(SUT2.isCardShowing(CARD_ID_1))
    }

    @Test
    fun removeTopShowingCardOf_whenCardIsNotTop_returnsFalse() {
        // Arrange
        SUT.add(MarketStackID.ROOT_1, createGameCards(listOf(mockGameCard1)))

        // Act
        val result = SUT.removeTopShowingCardOf(CARD_ID_2)

        // Assert
        assertFalse(result)
        assertTrue(SUT.isCardShowing(CARD_ID_1))
    }

    // endregion Card Query Tests

    // region Cost and Affordability Tests

    @Test
    fun isDieAvailable_whenDieExists_returnsTrue() {
        // Arrange
        SUT.setDiceCount(DieSides.D6, 2)

        // Act
        val isAvailable = SUT.isDieAvailable(6)

        // Assert
        assertTrue(isAvailable)
    }

    @Test
    fun isDieAvailable_whenDieDoesNotExist_returnsFalse() {
        // Act
        val isAvailable = SUT.isDieAvailable(6)

        // Assert
        assertFalse(isAvailable)
    }

    @Test
    fun getAffordableDice_returnsOnlyAffordableDice() {
        // Arrange
        SUT.setDiceCount(DieSides.D4, 2)
        SUT.setDiceCount(DieSides.D6, 2)
        SUT.setDiceCount(DieSides.D8, 2)
        SUT.setDiceCount(DieSides.D10, 2)
        SUT.setDiceCount(DieSides.D12, 2)
        SUT.setDiceCount(DieSides.D20, 2)

        // Act
        val affordableDice = SUT.getAffordableDice(5)

        // Assert
        assertEquals(listOf(4), affordableDice)
    }

    // endregion Cost and Affordability Tests
} 
