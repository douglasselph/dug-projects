package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.di.factory.DecisionDirectorFactory
import dugsolutions.leaf.di.factory.DieFactory
import dugsolutions.leaf.di.factory.DieFactoryRandom
import dugsolutions.leaf.di.factory.GameCardIDsFactory
import dugsolutions.leaf.di.factory.GameCardsFactory
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.components.FloralCount
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.domain.ExtendedHandItem
import dugsolutions.leaf.tool.Randomizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerTest {

    companion object {
        private const val CARD_ID_1 = 1
    }

    private lateinit var mockDeckManager: DeckManager
    private lateinit var deckManager: DeckManager
    private lateinit var mockRetainedComponents: StackManager
    private lateinit var mockFloralArray: FloralArray
    private lateinit var floralArray: FloralArray
    private lateinit var floralCount: FloralCount
    private lateinit var cardManager: CardManager
    private lateinit var mockDecisionDirectorFactory: DecisionDirectorFactory
    private lateinit var mockDecisionDirector: DecisionDirector
    private lateinit var sampleCard1: GameCard
    private lateinit var sampleCard2: GameCard
    private lateinit var D6: Die
    private lateinit var D4: Die
    private lateinit var D8: Die
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var startingDice: List<Die>
    private lateinit var mockDieFactory: DieFactory
    private lateinit var gameCardIDsFactory: GameCardIDsFactory
    private lateinit var mockGameChronicle: GameChronicle
    private lateinit var costScore: CostScore

    private lateinit var SUT: Player
    private lateinit var SUT2: Player

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        dieFactory = DieFactory(randomizer)
        costScore = CostScore()
        mockDeckManager = mockk(relaxed = true)
        mockFloralArray = mockk(relaxed = true)
        every { mockFloralArray.cards } returns emptyList()
        every { mockFloralArray.floralCount(any()) } returns 0

        val gameCardsFactory = GameCardsFactory(randomizer, costScore)
        cardManager = CardManager(gameCardsFactory)
        cardManager.loadCards(FakeCards.ALL_CARDS)
        gameCardIDsFactory = GameCardIDsFactory(cardManager, randomizer)
        deckManager = DeckManager(
            StackManager(cardManager, gameCardIDsFactory),
            StackManager(cardManager, gameCardIDsFactory),
            StackManager(cardManager, gameCardIDsFactory),
            dieFactory
        )
        floralCount = FloralCount()
        floralArray = FloralArray(cardManager, floralCount, gameCardIDsFactory)
        mockRetainedComponents = mockk(relaxed = true)
        mockDecisionDirector = mockk(relaxed = true)
        mockDecisionDirectorFactory = mockk(relaxed = true)
        every { mockDecisionDirectorFactory(any()) } returns mockDecisionDirector
        mockDieFactory = mockk(relaxed = true)
        sampleCard1 = FakeCards.fakeRoot
        sampleCard2 = FakeCards.fakeCanopy
        mockGameChronicle = mockk(relaxed = true)
        D4 = dieFactory(DieSides.D4)
        D6 = dieFactory(DieSides.D6)
        D8 = dieFactory(DieSides.D8)
        startingDice = dieFactory.startingDice
        every { mockDeckManager.drawCard() } returns CARD_ID_1
        every { mockDeckManager.handSize } returns Commons.HAND_SIZE
        every { mockDeckManager.addCardToHand(any()) } returns true

        every { mockDieFactory(DieSides.D4) } returns D4
        every { mockDieFactory(DieSides.D6) } returns D6
        every { mockDieFactory.startingDice } returns startingDice

        SUT = Player(
            mockDeckManager,
            mockFloralArray,
            cardManager,
            mockRetainedComponents,
            mockDieFactory,
            costScore,
            mockDecisionDirectorFactory
        )
        SUT2 = Player(
            deckManager,
            floralArray,
            cardManager,
            mockRetainedComponents,
            mockDieFactory,
            costScore,
            mockDecisionDirectorFactory
        )
    }

    @Test
    fun pipModifierAdd_addsValueToList() {
        // Arrange
        // Act
        SUT.pipModifier += 1
        SUT.pipModifier += 2

        // Assert
        assertEquals(3, SUT.pipModifier)
    }

    @Test
    fun pipTotal_returnsCorrectCombinedValue() {
        // Arrange
        SUT.pipModifier += 2
        SUT.pipModifier += -1
        every { mockDeckManager.pipTotal } returns 5

        // Act
        val total = SUT.pipTotal

        // Assert
        assertEquals(6, total)
    }

    @Test
    fun pipTotal_whenNoModifiers_returnsDeckManagerTotal() {
        // Arrange
        every { mockDeckManager.pipTotal } returns 7

        // Act
        val total = SUT.pipTotal

        // Assert
        assertEquals(7, total)
    }

    @Test
    fun pipTotal_withNegativeModifiers_reducesTotal() {
        // Arrange
        SUT.pipModifier += -3
        every { mockDeckManager.pipTotal } returns 5

        // Act
        val total = SUT.pipTotal

        // Assert
        assertEquals(2, total)
    }

    // endregion Properties and State Tests

    // region Hand Management Tests

    @Test
    fun hasCardInHand_returnsCorrectValue() {
        // Arrange
        every { mockDeckManager.hasCardInHand(CARD_ID_1) } returns true

        // Act
        val result = SUT.hasCardInHand(CARD_ID_1)

        // Assert
        assertTrue(result)
    }

    @Test
    fun hasDieInHand_returnsCorrectValue() {
        // Arrange
        every { mockDeckManager.hasDieInHand(D6) } returns true

        // Act
        val result = SUT.hasDieInHand(D6)

        // Assert
        assertTrue(result)
    }

    @Test
    fun getItemsInHand_returnsDeckManagerItems() {
        // Arrange
        val expectedItems = listOf(HandItem.Card(sampleCard1), HandItem.Dice(D6))
        every { mockDeckManager.getItemsInHand() } returns expectedItems

        // Act
        val result = SUT.getItemsInHand()

        // Assert
        assertEquals(expectedItems, result)
    }

    @Test
    fun discard_card_returnsCorrectValue() {
        // Arrange
        every { mockDeckManager.discard(CARD_ID_1) } returns true

        // Act
        val result = SUT.discard(CARD_ID_1)

        // Assert
        assertTrue(result)
    }

    @Test
    fun discard_die_returnsCorrectValue() {
        // Arrange
        every { mockDeckManager.discard(D6) } returns true

        // Act
        val result = SUT.discard(D6)

        // Assert
        assertTrue(result)
    }

    @Test
    fun removeCardFromHand_returnsCorrectValue() {
        // Arrange
        every { mockDeckManager.removeCardFromHand(CARD_ID_1) } returns true

        // Act
        val result = SUT.removeCardFromHand(CARD_ID_1)

        // Assert
        assertTrue(result)
    }

    @Test
    fun removeDieFromHand_returnsCorrectValue() {
        // Arrange
        every { mockDeckManager.removeDieFromHand(D6) } returns true

        // Act
        val result = SUT.removeDieFromHand(D6)

        // Assert
        assertTrue(result)
    }

    @Test
    fun retainCard_whenCardExists_returnsTrue() {
        // Arrange
        every { mockDeckManager.hasCardInHand(CARD_ID_1) } returns true
        every { mockDeckManager.discard(CARD_ID_1) } returns true
        every { mockRetainedComponents.addCard(CARD_ID_1) } returns true

        // Act
        val result = SUT.retainCard(CARD_ID_1)

        // Assert
        assertTrue(result)
    }

    @Test
    fun retainDie_whenDieExists_returnsTrue() {
        // Arrange
        every { mockDeckManager.hasDieInHand(D6) } returns true
        every { mockDeckManager.discard(D6) } returns true
        every { mockRetainedComponents.addDie(D6) } returns true

        // Act
        val result = SUT.retainDie(D6)

        // Assert
        assertTrue(result)
    }

    @Test
    fun discard_whenDieValueExists_returnsTrue() {
        // Arrange
        val dieValue = DieValue(6, 4)
        every { mockDeckManager.discard(dieValue) } returns true

        // Act
        val result = SUT.discard(dieValue)

        // Assert
        assertTrue(result)
        verify { mockDeckManager.discard(dieValue) }
    }

    @Test
    fun discard_whenDieValueDoesNotExist_returnsFalse() {
        // Arrange
        val dieValue = DieValue(6, 4)
        every { mockDeckManager.discard(dieValue) } returns false

        // Act
        val result = SUT.discard(dieValue)

        // Assert
        assertFalse(result)
        verify { mockDeckManager.discard(dieValue) }
    }

    @Test
    fun addCardToSupply_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.addCardToSupply(CARD_ID_1) } returns true

        // Act
        SUT.addCardToSupply(CARD_ID_1)

        // Assert
        verify { mockDeckManager.addCardToSupply(CARD_ID_1) }
    }

    @Test
    fun addDieToSupply_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.addDieToSupply(D6) } returns true

        // Act
        SUT.addDieToSupply(D6)

        // Assert
        verify { mockDeckManager.addDieToSupply(D6) }
    }

    @Test
    fun addDieToSupply_whenDieValue_delegatesToDeckManager() {
        // Arrange
        val dieValue = DieValue(6, 4)
        every { mockDeckManager.addDieToSupply(dieValue) } returns true

        // Act
        SUT.addDieToSupply(dieValue)

        // Assert
        verify { mockDeckManager.addDieToSupply(dieValue) }
    }

    @Test
    fun addDieToHand_whenDieValue_delegatesToDeckManager() {
        // Arrange
        val dieValue = DieValue(6, 4)
        every { mockDeckManager.addDieToHand(dieValue) } returns true

        // Act
        SUT.addDieToHand(dieValue)

        // Assert
        verify { mockDeckManager.addDieToHand(dieValue) }
    }

    // endregion Hand Management Tests

    // region Game Flow Tests

    @Test
    fun setup_initializesDeckAndDrawsInitialCards() {
        // Arrange
        val sampleSeedlings = GameCards(FakeCards.ALL_SEEDLINGS, randomizer, costScore)

        // Act
        SUT2.setupInitialDeck(sampleSeedlings)
        SUT2.drawHand(2)

        // Assert
        val cards = SUT2.cardsInHand
        val dice = SUT2.diceInHand
        assertEquals(2, cards.size)
        assertEquals(2, dice.size)
    }

    @Test
    fun drawHand_delegatesToDrawHandComponent() {
        // Arrange
        val preferredCardCount = 3
        every { mockDeckManager.handSize } returns 0
        every { mockDeckManager.drawCard() } returns CARD_ID_1
        every { mockDeckManager.drawDie() } returns D6

        // Act
        SUT.drawHand(preferredCardCount)

        // Assert
        verify { mockDeckManager.drawCard() }
        verify { mockDeckManager.drawDie() }
    }

    @Test
    fun discardHand_clearsAllState() {
        // Arrange
        SUT.incomingDamage = 5
        SUT.deflectDamage = 2
        SUT.pipModifier += 1
        SUT.cardsReused.add(sampleCard1)

        // Act
        SUT.discardHand()

        // Assert
        verify { mockDeckManager.discardHand() }
        assertEquals(0, SUT.incomingDamage)
        assertEquals(0, SUT.deflectDamage)
        assertEquals(0, SUT.pipModifier)
        assertEquals(0, SUT.cardsReused.size)
    }

    @Test
    fun reset_discardsHandAndResupplies() {
        // Arrange
        SUT.incomingDamage = 5
        SUT.deflectDamage = 2
        SUT.pipModifier += 1

        // Act
        SUT.reset()

        // Assert
        verify { mockDeckManager.discardHand() }
        verify { mockDeckManager.resupply() }
        assertEquals(0, SUT.incomingDamage)
        assertEquals(0, SUT.deflectDamage)
        assertEquals(0, SUT.pipModifier)
    }

    @Test
    fun resupply_callsDeckManagerResupply() {
        // Act
        SUT.resupply()

        // Assert
        verify(exactly = 1) { mockDeckManager.resupply() }
    }

    @Test
    fun trashSeedlingCards_delegatesToDeckManager() {
        // Act
        SUT.trashSeedlingCards()

        // Assert
        verify(exactly = 1) { mockDeckManager.trashSeedlingCards() }
    }

    // endregion Game Flow Tests

    // region Floral Array Tests

    @Test
    fun flowerCount_whenNoMatchingFlowers_returnsZero() {
        // Arrange
        val bloomCard = FakeCards.fakeBloom
        every { mockFloralArray.floralCount(any()) } returns 0

        // Act
        val result = SUT.flowerCount(bloomCard)

        // Assert
        assertEquals(0, result)
        assertTrue(bloomCard.matchWith is MatchWith.Flower)
        val flowerCardId = (bloomCard.matchWith as MatchWith.Flower).flowerCardId
        verify { mockFloralArray.floralCount(flowerCardId) }
    }

    @Test
    fun flowerCount_whenMatchingFlowersExist_returnsCorrectCount() {
        // Arrange
        val bloomCard = FakeCards.fakeBloom
        val expectedCount = 3
        every { mockFloralArray.floralCount(any()) } returns expectedCount

        // Act
        val result = SUT.flowerCount(bloomCard)

        // Assert
        assertEquals(expectedCount, result)
        val flowerCardId = (bloomCard.matchWith as MatchWith.Flower).flowerCardId
        verify { mockFloralArray.floralCount(flowerCardId) }
    }

    @Test
    fun floralCards_returnsFloralArrayCards() {
        // Arrange
        val expectedCards = listOf(FakeCards.fakeFlower)
        every { mockFloralArray.cards } returns expectedCards

        // Act
        val result = SUT.floralCards

        // Assert
        assertEquals(expectedCards, result)
    }

    @Test
    fun addCardToFloralArray_delegatesToFloralArray() {
        // Arrange
        val cardId = CARD_ID_1

        // Act
        SUT.addCardToFloralArray(cardId)

        // Assert
        verify { mockFloralArray.add(cardId) }
    }

    @Test
    fun clearFloralCards_delegatesToFloralArray() {
        // Act
        SUT.clearFloralCards()

        // Assert
        verify { mockFloralArray.clear() }
    }

    @Test
    fun removeCardFromFloralArray_delegatesToFloralArray() {
        // Arrange
        val cardId = CARD_ID_1
        every { mockFloralArray.remove(cardId) } returns true

        // Act
        val result = SUT.removeCardFromFloralArray(cardId)

        // Assert
        assertTrue(result)
        verify { mockFloralArray.remove(cardId) }
    }

    @Test
    fun removeCardFromFloralArray_whenCardNotPresent_returnsFalse() {
        // Arrange
        val cardId = CARD_ID_1
        every { mockFloralArray.remove(cardId) } returns false

        // Act
        val result = SUT.removeCardFromFloralArray(cardId)

        // Assert
        assertFalse(result)
        verify { mockFloralArray.remove(cardId) }
    }

    // endregion Floral Array Tests

    @Test
    fun getExtendedItems_returnsCombinedHandAndFloralItems() {
        // Arrange
        val handItems = listOf(
            HandItem.Card(sampleCard1),
            HandItem.Dice(D6)
        )
        val floralCards = listOf(sampleCard2)
        
        every { mockDeckManager.getItemsInHand() } returns handItems
        every { mockFloralArray.cards } returns floralCards
        
        // Act
        val result = SUT.getExtendedItems()
        
        // Assert
        assertEquals(3, result.size)
        assertTrue(result[0] is ExtendedHandItem.Card)
        assertTrue(result[1] is ExtendedHandItem.Dice)
        assertTrue(result[2] is ExtendedHandItem.FloralArray)
        
        assertEquals(sampleCard1, (result[0] as ExtendedHandItem.Card).card)
        assertEquals(D6, (result[1] as ExtendedHandItem.Dice).die)
        assertEquals(sampleCard2, (result[2] as ExtendedHandItem.FloralArray).card)
    }

}
