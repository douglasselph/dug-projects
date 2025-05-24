package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.DecisionDirectorFactory
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.di.GameCardIDsFactory
import dugsolutions.leaf.di.GameCardsFactory
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.tool.Randomizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawHandTest {

    private lateinit var deckManager: DeckManager
    private lateinit var cardManager: CardManager
    private lateinit var mockRetainedComponents: StackManager
    private lateinit var mockDecisionDirectorFactory: DecisionDirectorFactory
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var D6: Die
    private lateinit var D4: Die
    private lateinit var D8: Die
    private lateinit var startingDice: List<Die>
    private lateinit var gameCardIDsFactory: GameCardIDsFactory
    private lateinit var mockGameChronicle: GameChronicle
    private lateinit var costScore: CostScore
    private lateinit var drawHand: DrawHand

    private lateinit var SUT: Player

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)
        costScore = CostScore()
        val gameCardsFactory = GameCardsFactory(randomizer, costScore)
        cardManager = CardManager(gameCardsFactory)
        cardManager.loadCards(FakeCards.ALL_CARDS)
        gameCardIDsFactory = GameCardIDsFactory(cardManager, randomizer)
        deckManager = DeckManager(
            StackManager(cardManager, gameCardIDsFactory),
            StackManager(cardManager, gameCardIDsFactory),
            StackManager(cardManager, gameCardIDsFactory),
            StackManager(cardManager, gameCardIDsFactory),
            dieFactory
        )
        mockRetainedComponents = mockk(relaxed = true)
        mockDecisionDirectorFactory = mockk(relaxed = true)
        mockGameChronicle = mockk(relaxed = true)
        D4 = dieFactory(DieSides.D4)
        D6 = dieFactory(DieSides.D6)
        D8 = dieFactory(DieSides.D8)
        startingDice = dieFactory.startingDice
        drawHand = DrawHand(mockGameChronicle)

        SUT = Player(
            deckManager,
            cardManager,
            mockRetainedComponents,
            dieFactory,
            costScore,
            drawHand,
            mockDecisionDirectorFactory
        )
    }

    @Test
    fun invoke_whenSupplyEmpty_resuppliesFirst() {
        // Arrange
        every { deckManager.isSupplyEmpty } returns true
        every { deckManager.drawCard() } returns FakeCards.fakeRoot.id
        every { deckManager.drawDie() } returns D6
        every { deckManager.getItemsInHand() } returns emptyList()
        every { deckManager.handSize } returns 0

        // Act
        drawHand(SUT, 1)

        // Assert
        verify { deckManager.resupply() }
        verify { deckManager.drawCard() }
        verify { deckManager.drawDie() }
    }

    @Test
    fun invoke_whenLowSupply_takesAllSupplyFirst() {
        // Arrange
        SUT.addCardToSupply(FakeCards.fakeRoot.id)
        SUT.addDieToSupply(D6)
        SUT.addCardToCompost(FakeCards.fakeRoot2.id)
        SUT.addDieToCompost(D4)

        // Act
        drawHand(SUT, 3) // Try to draw 3 cards, but supply only has 1 card and 1 die

        // Assert
        assertEquals(2, SUT.cardsInHand.size)
        assertTrue(SUT.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(SUT.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, SUT.diceInHand.size)
        assertTrue(SUT.diceInHand.dice.contains(D6))
        assertTrue(SUT.diceInHand.dice.contains(D4))
    }

    @Test
    fun invoke_whenLowSupplyAndPreferredCountNotMet_triesToDrawMoreCards() {
        // Arrange
        SUT.addCardToSupply(FakeCards.fakeRoot.id)
        SUT.addDieToSupply(D6)
        SUT.addCardToCompost(FakeCards.fakeRoot2.id)
        SUT.addCardToCompost(FakeCards.fakeVine.id)
        SUT.addDieToCompost(D4)
        SUT.addDieToCompost(D8)

        // Act
        drawHand(SUT, 2) // Try to draw 2 cards, but supply only has 1 card and 1 die

        // Assert
        assertEquals(2, SUT.cardsInHand.size)
        assertTrue(SUT.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(SUT.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, SUT.diceInHand.size)
        assertTrue(SUT.diceInHand.dice.contains(D6))
        assertTrue(SUT.diceInHand.dice.contains(D4))
    }

    @Test
    fun invoke_whenLowSupplyAndPreferredCountMet_fillsWithDice() {
        // Arrange
        SUT.addCardToSupply(FakeCards.fakeRoot.id)
        SUT.addCardToSupply(FakeCards.fakeRoot2.id)
        SUT.addDieToSupply(D6)
        SUT.addCardToCompost(FakeCards.fakeBloom.id)
        SUT.addCardToCompost(FakeCards.fakeVine.id)
        SUT.addDieToCompost(D4)
        SUT.addDieToCompost(D8)

        // Act
        drawHand(SUT, 1) // Try to draw 1 card, but supply has 2 cards and 1 die

        // Assert
        assertEquals(2, SUT.cardsInHand.size)
        assertTrue(SUT.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(SUT.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, SUT.diceInHand.size)
        assertTrue(SUT.diceInHand.dice.contains(D6))
        assertTrue(SUT.diceInHand.dice.contains(D4))
    }

    @Test
    fun invoke_whenSupplyEmptyAndCompostAvailable_usesCompost() {
        // Arrange
        SUT.addCardToCompost(FakeCards.fakeRoot.id)
        SUT.addCardToCompost(FakeCards.fakeRoot2.id)
        SUT.addCardToCompost(FakeCards.fakeBloom.id)
        SUT.addCardToCompost(FakeCards.fakeVine.id)
        SUT.addDieToCompost(D4)
        SUT.addDieToCompost(D6)
        SUT.addDieToCompost(D8)

        // Act
        drawHand(SUT, 1)

        // Assert
        assertEquals(1, SUT.cardsInHand.size)
        assertTrue(SUT.cardsInHand.contains(FakeCards.fakeRoot))
        assertEquals(3, SUT.diceInHand.size)
        assertTrue(SUT.diceInHand.dice.contains(D6))
        assertTrue(SUT.diceInHand.dice.contains(D4))
        assertTrue(SUT.diceInHand.dice.contains(D8))
    }

    @Test
    fun invoke_whenLowSupplyAndPartialHand_considersExistingHand() {
        // Arrange
        SUT.addCardToHand(FakeCards.fakeRoot.id)
        SUT.addCardToSupply(FakeCards.fakeRoot2.id)
        SUT.addCardToCompost(FakeCards.fakeBloom.id)
        SUT.addCardToCompost(FakeCards.fakeVine.id)
        SUT.addDieToSupply(D4)
        SUT.addDieToSupply(D6)
        SUT.addDieToCompost(D8)

        // Act
        drawHand(SUT, 3) // Try to draw 3 cards, but already have 1 card and supply has 1 card

        // Assert
        assertEquals(2, SUT.cardsInHand.size)
        assertTrue(SUT.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(SUT.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, SUT.diceInHand.size)
        assertTrue(SUT.diceInHand.dice.contains(D6))
        assertTrue(SUT.diceInHand.dice.contains(D4))
    }
} 
