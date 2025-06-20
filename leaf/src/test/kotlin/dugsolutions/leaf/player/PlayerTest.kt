package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.di.GameCardIDsFactory
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.components.DrawNewHand
import dugsolutions.leaf.player.effect.FloralBonusCount
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.player.domain.ExtendedHandItem
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.DieValue
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

    private val mockDeckManager: DeckManager = mockk(relaxed = true)
    private lateinit var deckManager: DeckManager
    private val mockFloralArray: FloralArray = mockk(relaxed = true)
    private val mockFloralBonusCount: FloralBonusCount = mockk(relaxed = true)
    private lateinit var floralArray: FloralArray
    private lateinit var floralBonusCount: FloralBonusCount
    private lateinit var cardManager: CardManager
    private val mockDecisionDirector: DecisionDirector = mockk(relaxed = true)
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
    private val drawNewHand = DrawNewHand()
    private val costScore: CostScore = CostScore()

    private lateinit var SUT: Player
    private lateinit var SUT2: Player

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        dieFactory = DieFactory(randomizer)
        every { mockFloralArray.cards } returns emptyList()
        every { mockFloralBonusCount(any(), any()) } returns 0

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
        floralBonusCount = FloralBonusCount()
        floralArray = FloralArray(cardManager, gameCardIDsFactory)
        mockDieFactory = mockk(relaxed = true)
        sampleCard1 = FakeCards.rootCard
        sampleCard2 = FakeCards.canopyCard
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
            mockFloralBonusCount,
            cardManager,
            mockDieFactory,
            costScore,
            drawNewHand,
            mockDecisionDirector
        )
        SUT2 = Player(
            deckManager,
            floralArray,
            floralBonusCount,
            cardManager,
            mockDieFactory,
            costScore,
            drawNewHand,
            mockDecisionDirector
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

    @Test
    fun isResupplyNeeded_whenDeckManagerNeedsResupply_returnsTrue() {
        // Arrange
        every { mockDeckManager.isResupplyNeeded } returns true

        // Act
        val result = SUT.isResupplyNeeded

        // Assert
        assertTrue(result)
        verify { mockDeckManager.isResupplyNeeded }
    }

    @Test
    fun isResupplyNeeded_whenDeckManagerDoesNotNeedResupply_returnsFalse() {
        // Arrange
        every { mockDeckManager.isResupplyNeeded } returns false

        // Act
        val result = SUT.isResupplyNeeded

        // Assert
        assertFalse(result)
        verify { mockDeckManager.isResupplyNeeded }
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
        val expectedItems = listOf(HandItem.aCard(sampleCard1), HandItem.aDie(D6))
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
    fun retainCard_whenCardInHand_removesFromHandAndAddsToRetained() {
        // Arrange
        every { mockDeckManager.hasCardInHand(sampleCard1.id) } returns true
        every { mockDeckManager.removeCardFromHand(sampleCard1.id) } returns true

        // Act
        val result = SUT.retainCard(sampleCard1)

        // Assert
        assertTrue(result)
        verify { mockDeckManager.hasCardInHand(sampleCard1.id) }
        verify { mockDeckManager.removeCardFromHand(sampleCard1.id) }
        assertEquals(1, SUT.retained.size)
        assertTrue(SUT.retained[0] is HandItem.aCard)
        assertEquals(sampleCard1, (SUT.retained[0] as HandItem.aCard).card)
    }

    @Test
    fun retainCard_whenCardNotInHand_returnsFalseAndDoesNotModifyRetained() {
        // Arrange
        every { mockDeckManager.hasCardInHand(sampleCard1.id) } returns false

        // Act
        val result = SUT.retainCard(sampleCard1)

        // Assert
        assertFalse(result)
        verify { mockDeckManager.hasCardInHand(sampleCard1.id) }
        verify(exactly = 0) { mockDeckManager.removeCardFromHand(any()) }
        assertTrue(SUT.retained.isEmpty())
    }

    @Test
    fun retainDie_whenDieInHand_removesFromHandAndAddsToRetained() {
        // Arrange
        every { mockDeckManager.hasDieInHand(D6) } returns true
        every { mockDeckManager.removeDieFromHand(D6) } returns true

        // Act
        val result = SUT.retainDie(D6)

        // Assert
        assertTrue(result)
        verify { mockDeckManager.hasDieInHand(D6) }
        verify { mockDeckManager.removeDieFromHand(D6) }
        assertEquals(1, SUT.retained.size)
        assertTrue(SUT.retained[0] is HandItem.aDie)
        assertEquals(D6, (SUT.retained[0] as HandItem.aDie).die)
    }

    @Test
    fun retainDie_whenDieNotInHand_returnsFalseAndDoesNotModifyRetained() {
        // Arrange
        every { mockDeckManager.hasDieInHand(D6) } returns false

        // Act
        val result = SUT.retainDie(D6)

        // Assert
        assertFalse(result)
        verify { mockDeckManager.hasDieInHand(D6) }
        verify(exactly = 0) { mockDeckManager.removeDieFromHand(any()) }
        assertTrue(SUT.retained.isEmpty())
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
    fun discardHand_callsJustDeckManager() {
        // Arrange
        // Act
        SUT.discardHand()

        // Assert
        verify { mockDeckManager.discardHand() }
    }

    @Test
    fun clearEffects_allExpectedCleared() {
        // Arrange
        SUT.incomingDamage = 5
        SUT.deflectDamage = 2
        SUT.pipModifier += 1
        SUT.reused.add(HandItem.aCard(sampleCard1))
        SUT.retained.add(HandItem.aCard(sampleCard2))
        SUT.cardsToPlay.add(sampleCard1)
        SUT.delayedEffectList.add(AppliedEffect.FlourishOverride)

        // Act
        SUT.clearEffects()

        // Assert
        assertEquals(0, SUT.incomingDamage)
        assertEquals(0, SUT.deflectDamage)
        assertEquals(0, SUT.pipModifier)
        assertEquals(0, SUT.reused.size)
        assertEquals(0, SUT.retained.size)
        assertEquals(0, SUT.cardsToPlay.size)
        assertEquals(0, SUT.delayedEffectList.size)
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
        val bloomCard = FakeCards.bloomCard
        val floralCards = listOf(FakeCards.flowerCard2.id)
        val handFlowers = listOf(HandItem.aCard(FakeCards.flowerCard3))
        every { mockDeckManager.getItemsInHand() } returns handFlowers
        every { mockFloralBonusCount(any(), any()) } returns 0

        // Act
        val result = SUT.flowerCount(floralCards, bloomCard)

        // Assert
        assertEquals(0, result)
        assertTrue(bloomCard.matchWith is MatchWith.Flower)
        val flowerCardId = (bloomCard.matchWith as MatchWith.Flower).flowerCardId
        verify { mockFloralBonusCount(any(), flowerCardId) }
    }

    @Test
    fun flowerCount_whenMatchingFlowersExist_returnsCorrectCount() {
        // Arrange
        val bloomCard = FakeCards.bloomCard
        val floralCards = listOf(FakeCards.flowerCard.id)
        val handFlowers = listOf(HandItem.aCard(FakeCards.flowerCard))
        every { mockDeckManager.getItemsInHand() } returns handFlowers
        every { mockFloralBonusCount(any(), any()) } returns 2

        // Act
        val result = SUT.flowerCount(floralCards, bloomCard)

        // Assert
        assertEquals(2, result)
        val flowerCardId = (bloomCard.matchWith as MatchWith.Flower).flowerCardId
        verify { mockFloralBonusCount(any(), flowerCardId) }
    }

    @Test
    fun flowerCount_whenCardIsNotBloom_returnsZero() {
        // Arrange
        val nonBloomCard = FakeCards.rootCard // A card without MatchWith.Flower
        val floralCards = listOf(FakeCards.flowerCard.id)
        val handFlowers = listOf(HandItem.aCard(FakeCards.flowerCard))
        every { mockDeckManager.getItemsInHand() } returns handFlowers

        // Act
        val result = SUT.flowerCount(floralCards, nonBloomCard)

        // Assert
        assertEquals(0, result)
        verify(exactly = 0) { mockFloralBonusCount(any(), any()) }
    }

    @Test
    fun floralCards_returnsFloralArrayCards() {
        // Arrange
        val expectedCards = listOf(FakeCards.flowerCard)
        every { mockFloralArray.cards } returns expectedCards

        // Act
        val result = SUT.floralCards

        // Assert
        assertEquals(expectedCards, result)
    }

    @Test
    fun addCardToFloralArray_delegatesToBuddingStack() {
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
    fun removeCardFromFloralArray_delegatesToBuddingStack() {
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
    fun getExtendedItems_returnsCombinedHandAndFloralHandItems() {
        // Arrange
        val handItems = listOf(
            HandItem.aCard(sampleCard1),
            HandItem.aDie(D6)
        )
        val floralCards = listOf(sampleCard2)

        every { mockDeckManager.getItemsInHand() } returns handItems
        every { mockFloralArray.cards } returns floralCards

        // Act
        val result = SUT.getExtendedHandItems()

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
