package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.di.DecisionDirectorFactory
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.di.GameCardIDsFactory
import dugsolutions.leaf.di.GameCardsFactory
import dugsolutions.leaf.game.purchase.evaluator.PurchaseCardEvaluator
import dugsolutions.leaf.game.purchase.evaluator.PurchaseDieEvaluator
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.DecisionDrawCount
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
    private lateinit var gameChronicle: GameChronicle
    private lateinit var costScore: CostScore

    private lateinit var SUT: Player
    private lateinit var SUT2: Player

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)
        costScore = CostScore()
        mockDeckManager = mockk(relaxed = true)
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
        mockRetainedComponents = mockk(relaxed = true)
        mockDecisionDirector = mockk(relaxed = true)
        mockDecisionDirectorFactory = mockk(relaxed = true)
        every { mockDecisionDirectorFactory(any()) } returns mockDecisionDirector
        mockDieFactory = mockk(relaxed = true)
        sampleCard1 = FakeCards.fakeRoot
        sampleCard2 = FakeCards.fakeCanopy
        gameChronicle = mockk(relaxed = true)
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
            cardManager,
            mockRetainedComponents,
            mockDieFactory,
            gameChronicle,
            costScore,
            mockDecisionDirectorFactory,
        )
        SUT2 = Player(
            deckManager,
            cardManager,
            mockRetainedComponents,
            mockDieFactory,
            gameChronicle,
            costScore,
            mockDecisionDirectorFactory
        )
    }

    // region Properties and State Tests

    @Test
    fun bloomCount_returnsDeckManagerBloomCount() {
        // Arrange
        every { mockDeckManager.bloomCount } returns 3

        // Act
        val result = SUT.bloomCount

        // Assert
        assertEquals(3, result)
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
        SUT2.draw(2)

        // Assert
        val cards = SUT2.cardsInHand
        val dice = SUT2.diceInHand
        assertEquals(2, cards.size)
        assertEquals(2, dice.size)
    }

    @Test
    fun draw_whenSupplyEmpty_resuppliesFirst() {
        // Arrange
        every { mockDeckManager.isSupplyEmpty } returns true
        every { mockDeckManager.drawCard() } returns CARD_ID_1
        every { mockDeckManager.drawDie() } returns D6
        every { mockDeckManager.getItemsInHand() } returns emptyList()
        every { mockDeckManager.handSize } returns 0

        // Act
        SUT.draw(1)

        // Assert
        verify { mockDeckManager.resupply() }
        verify { mockDeckManager.drawCard() }
        verify { mockDeckManager.drawDie() }
    }

    @Test
    fun drawCard_whenSupplyEmpty_resuppliesFirst() {
        // Arrange
        every { mockDeckManager.isSupplyEmpty } returns true
        every { mockDeckManager.drawCard() } returns CARD_ID_1

        // Act
        val result = SUT.drawCard()

        // Assert
        verify { mockDeckManager.resupply() }
        assertEquals(CARD_ID_1, result)
    }

    @Test
    fun drawDie_whenHandEmpty_resuppliesFirst() {
        // Arrange
        every { mockDeckManager.isSupplyEmpty } returns true
        every { mockDeckManager.drawDie() } returns D6

        // Act
        val result = SUT.drawDie()

        // Assert
        verify { mockDeckManager.resupply() }
        assertEquals(D6, result)
    }

    @Test
    fun discardHand_clearsAllState() {
        // Arrange
        SUT.incomingDamage = 5
        SUT.thornDamage = 3
        SUT.deflectDamage = 2
        SUT.pipModifier += 1
        SUT.cardsReused.add(sampleCard1)

        // Act
        SUT.discardHand()

        // Assert
        verify { mockDeckManager.discardHand() }
        assertEquals(0, SUT.incomingDamage)
        assertEquals(0, SUT.thornDamage)
        assertEquals(0, SUT.deflectDamage)
        assertEquals(0, SUT.pipModifier)
        assertEquals(0, SUT.cardsReused.size)
    }

    @Test
    fun reset_discardsHandAndResupplies() {
        // Arrange
        SUT.incomingDamage = 5
        SUT.thornDamage = 3
        SUT.deflectDamage = 2
        SUT.pipModifier += 1

        // Act
        SUT.reset()

        // Assert
        verify { mockDeckManager.discardHand() }
        verify { mockDeckManager.resupply() }
        assertEquals(0, SUT.incomingDamage)
        assertEquals(0, SUT.thornDamage)
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

    @Test
    fun drawHand_callsDrawWithCountFromDecision() {
        // Arrange
        SUT.discardHand()
        val cardCount = 3
        val sampleResult = cardCount
        every { mockDecisionDirector.drawCountDecision() } returns sampleResult
        every { mockDeckManager.handSize } returns 0

        // Act
        SUT.drawHand()

        // Assert
        verify(exactly = 3) { mockDeckManager.drawCard() }
    }

    @Test
    fun draw_whenLowSupply_takesAllSupplyFirst() {
        // Arrange
        SUT2.addCardToSupply(FakeCards.fakeRoot.id)
        SUT2.addDieToSupply(D6)
        SUT2.addCardToCompost(FakeCards.fakeRoot2.id)
        SUT2.addDieToCompost(D4)

        // Act
        SUT2.draw(3) // Try to draw 3 cards, but supply only has 1 card and 1 die

        // Assert
        assertEquals(2, SUT2.cardsInHand.size)
        assertTrue(SUT2.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(SUT2.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, SUT2.diceInHand.size)
        assertTrue(SUT2.diceInHand.dice.contains(D6))
        assertTrue(SUT2.diceInHand.dice.contains(D4))
    }

    @Test
    fun draw_whenLowSupplyAndPreferredCountNotMet_triesToDrawMoreCards() {
        // Arrange
        SUT2.addCardToSupply(FakeCards.fakeRoot.id)
        SUT2.addDieToSupply(D6)
        SUT2.addCardToCompost(FakeCards.fakeRoot2.id)
        SUT2.addCardToCompost(FakeCards.fakeVine.id)
        SUT2.addDieToCompost(D4)
        SUT2.addDieToCompost(D8)

        // Act
        SUT2.draw(2) // Try to draw 2 cards, but supply only has 1 card and 1 die

        // Assert
        assertEquals(2, SUT2.cardsInHand.size)
        assertTrue(SUT2.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(SUT2.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, SUT2.diceInHand.size)
        assertTrue(SUT2.diceInHand.dice.contains(D6))
        assertTrue(SUT2.diceInHand.dice.contains(D4))
    }

    @Test
    fun draw_whenLowSupplyAndPreferredCountMet_fillsWithDice() {
        // Arrange
        SUT2.addCardToSupply(FakeCards.fakeRoot.id)
        SUT2.addCardToSupply(FakeCards.fakeRoot2.id)
        SUT2.addDieToSupply(D6)
        SUT2.addCardToCompost(FakeCards.fakeBloom.id)
        SUT2.addCardToCompost(FakeCards.fakeVine.id)
        SUT2.addDieToCompost(D4)
        SUT2.addDieToCompost(D8)

        // Act
        SUT2.draw(1) // Try to draw 1 card, but supply has 2 cards and 1 die

        // Assert
        assertEquals(2, SUT2.cardsInHand.size)
        assertTrue(SUT2.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(SUT2.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, SUT2.diceInHand.size)
        assertTrue(SUT2.diceInHand.dice.contains(D6))
        assertTrue(SUT2.diceInHand.dice.contains(D4))
    }

    @Test
    fun draw_whenSupplyEmptyAndCompostAvailable_usesCompost() {
        // Arrange
        SUT2.addCardToCompost(FakeCards.fakeRoot.id)
        SUT2.addCardToCompost(FakeCards.fakeRoot2.id)
        SUT2.addCardToCompost(FakeCards.fakeBloom.id)
        SUT2.addCardToCompost(FakeCards.fakeVine.id)
        SUT2.addDieToCompost(D4)
        SUT2.addDieToCompost(D6)
        SUT2.addDieToCompost(D8)

        // Act
        SUT2.draw(1)

        // Assert
        assertEquals(1, SUT2.cardsInHand.size)
        assertTrue(SUT2.cardsInHand.contains(FakeCards.fakeRoot))
        assertEquals(3, SUT2.diceInHand.size)
        assertTrue(SUT2.diceInHand.dice.contains(D6))
        assertTrue(SUT2.diceInHand.dice.contains(D4))
        assertTrue(SUT2.diceInHand.dice.contains(D8))
    }

    @Test
    fun draw_whenLowSupplyAndPartialHand_considersExistingHand() {
        // Arrange
        SUT2.addCardToHand(FakeCards.fakeRoot.id)
        SUT2.addCardToSupply(FakeCards.fakeRoot2.id)
        SUT2.addCardToCompost(FakeCards.fakeBloom.id)
        SUT2.addCardToCompost(FakeCards.fakeVine.id)
        SUT2.addDieToSupply(D4)
        SUT2.addDieToSupply(D6)
        SUT2.addDieToCompost(D8)

        // Act
        SUT2.draw(3) // Try to draw 3 cards, but already have 1 card and supply has 1 card

        // Assert
        assertEquals(2, SUT2.cardsInHand.size)
        assertTrue(SUT2.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(SUT2.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, SUT2.diceInHand.size)
        assertTrue(SUT2.diceInHand.dice.contains(D6))
        assertTrue(SUT2.diceInHand.dice.contains(D4))
    }

    // endregion Game Flow Tests

}
