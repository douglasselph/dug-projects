package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.factory.DecisionDirectorFactory
import dugsolutions.leaf.di.factory.DieFactory
import dugsolutions.leaf.di.factory.DieFactoryRandom
import dugsolutions.leaf.di.factory.GameCardIDsFactory
import dugsolutions.leaf.di.factory.GameCardsFactory
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.Randomizer
import dugsolutions.leaf.tool.RandomizerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawHandTest {

    private lateinit var deckManager: DeckManager
    private lateinit var mockDeckManager: DeckManager
    private lateinit var floralCount: FloralCount
    private lateinit var floralArray: FloralArray
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

    private lateinit var samplePlayer: Player
    private lateinit var samplePlayer2: Player

    @BeforeEach
    fun setup() {
        randomizer = RandomizerTD()
        dieFactory = DieFactory(randomizer)
        costScore = CostScore()
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
        mockDeckManager = mockk(relaxed = true)
        floralCount = FloralCount()
        floralArray = FloralArray(cardManager, floralCount, gameCardIDsFactory)
        mockRetainedComponents = mockk(relaxed = true)
        mockDecisionDirectorFactory = mockk(relaxed = true)
        mockGameChronicle = mockk(relaxed = true)
        D4 = dieFactory(DieSides.D4)
        D6 = dieFactory(DieSides.D6)
        D8 = dieFactory(DieSides.D8)
        startingDice = dieFactory.startingDice

        samplePlayer = Player(
            mockDeckManager,
            floralArray,
            cardManager,
            mockRetainedComponents,
            dieFactory,
            costScore,
            mockDecisionDirectorFactory
        )
        samplePlayer2 = Player(
            deckManager,
            floralArray,
            cardManager,
            mockRetainedComponents,
            dieFactory,
            costScore,
            mockDecisionDirectorFactory
        )
    }

    @Test
    fun invoke_whenSupplyEmpty_resuppliesFirst() {
        // Arrange
        every { mockDeckManager.isSupplyEmpty } returns true
        every { mockDeckManager.drawCard() } returns FakeCards.fakeRoot.id
        every { mockDeckManager.drawDie() } returns D6
        every { mockDeckManager.getItemsInHand() } returns emptyList()
        every { mockDeckManager.handSize } returns 0

        // Act
        samplePlayer.drawHand(1)

        // Assert
        verify { mockDeckManager.resupply() }
        verify { mockDeckManager.drawCard() }
        verify { mockDeckManager.drawDie() }
    }

    @Test
    fun invoke_whenLowSupply_takesAllSupplyFirst() {
        // Arrange
        samplePlayer2.addCardToSupply(FakeCards.fakeRoot.id)
        samplePlayer2.addDieToSupply(D6)
        samplePlayer2.addCardToCompost(FakeCards.fakeRoot2.id)
        samplePlayer2.addDieToCompost(D4)

        // Act
        samplePlayer2.drawHand(3) // Try to draw 3 cards, but supply only has 1 card and 1 die

        // Assert
        assertEquals(2, samplePlayer2.cardsInHand.size)
        assertTrue(samplePlayer2.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(samplePlayer2.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, samplePlayer2.diceInHand.size)
        assertTrue(samplePlayer2.diceInHand.dice.contains(D6))
        assertTrue(samplePlayer2.diceInHand.dice.contains(D4))
    }

    @Test
    fun invoke_whenLowSupplyAndPreferredCountNotMet_triesToDrawMoreCards() {
        // Arrange
        samplePlayer2.addCardToSupply(FakeCards.fakeRoot.id)
        samplePlayer2.addDieToSupply(D6)
        samplePlayer2.addCardToCompost(FakeCards.fakeRoot2.id)
        samplePlayer2.addCardToCompost(FakeCards.fakeVine.id)
        samplePlayer2.addDieToCompost(D4)
        samplePlayer2.addDieToCompost(D8)

        // Act
        samplePlayer2.drawHand(2) // Try to draw 2 cards, but supply only has 1 card and 1 die

        // Assert
        assertEquals(2, samplePlayer2.cardsInHand.size)
        assertEquals(FakeCards.fakeRoot.id, samplePlayer2.cardsInHand[0].id)
        assertEquals(FakeCards.fakeVine.id, samplePlayer2.cardsInHand[1].id) // Deck shuffled is why this and not fakeRoot2
        assertEquals(2, samplePlayer2.diceInHand.size)
        assertEquals(D4, samplePlayer2.diceInHand.dice[0])
        assertEquals(D6, samplePlayer2.diceInHand.dice[1])
    }

    @Test
    fun invoke_whenLowSupplyAndPreferredCountMet_fillsWithDice() {
        // Arrange
        samplePlayer2.addCardToSupply(FakeCards.fakeRoot.id)
        samplePlayer2.addCardToSupply(FakeCards.fakeRoot2.id)
        samplePlayer2.addDieToSupply(D6)
        samplePlayer2.addCardToCompost(FakeCards.fakeBloom.id)
        samplePlayer2.addCardToCompost(FakeCards.fakeVine.id)
        samplePlayer2.addDieToCompost(D4)
        samplePlayer2.addDieToCompost(D8)

        // Act
        samplePlayer2.drawHand(1) // Try to draw 1 card, but supply has 2 cards and 1 die

        // Assert
        assertEquals(2, samplePlayer2.cardsInHand.size)
        assertTrue(samplePlayer2.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(samplePlayer2.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, samplePlayer2.diceInHand.size)
        assertTrue(samplePlayer2.diceInHand.dice.contains(D6))
        assertTrue(samplePlayer2.diceInHand.dice.contains(D4))
    }

    @Test
    fun invoke_whenSupplyEmptyAndCompostAvailable_usesCompost() {
        // Arrange
        samplePlayer2.addCardToCompost(FakeCards.fakeRoot.id)
        samplePlayer2.addCardToCompost(FakeCards.fakeRoot2.id)
        samplePlayer2.addCardToCompost(FakeCards.fakeBloom.id)
        samplePlayer2.addCardToCompost(FakeCards.fakeVine.id)
        samplePlayer2.addDieToCompost(D4)
        samplePlayer2.addDieToCompost(D6)
        samplePlayer2.addDieToCompost(D8)

        // Act
        samplePlayer2.drawHand(1)

        // Assert
        assertEquals(1, samplePlayer2.cardsInHand.size)
        assertEquals(FakeCards.fakeRoot.id, samplePlayer2.cardsInHand[0].id)
        assertEquals(3, samplePlayer2.diceInHand.size)
        assertEquals(D4, samplePlayer2.diceInHand.dice[0])
        assertEquals(D6, samplePlayer2.diceInHand.dice[1])
        assertEquals(D8, samplePlayer2.diceInHand.dice[2])
    }

    @Test
    fun invoke_whenLowSupplyAndPartialHand_considersExistingHand() {
        // Arrange
        samplePlayer2.addCardToHand(FakeCards.fakeRoot.id)
        samplePlayer2.addCardToSupply(FakeCards.fakeRoot2.id)
        samplePlayer2.addCardToCompost(FakeCards.fakeBloom.id)
        samplePlayer2.addCardToCompost(FakeCards.fakeVine.id)
        samplePlayer2.addDieToSupply(D4)
        samplePlayer2.addDieToSupply(D6)
        samplePlayer2.addDieToCompost(D8)

        // Act
        samplePlayer2.drawHand(3) // Try to draw 3 cards, but already have 1 card and supply has 1 card

        // Assert
        assertEquals(2, samplePlayer2.cardsInHand.size)
        assertTrue(samplePlayer2.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(samplePlayer2.cardsInHand.contains(FakeCards.fakeRoot2))
        assertEquals(2, samplePlayer2.diceInHand.size)
        assertTrue(samplePlayer2.diceInHand.dice.contains(D6))
        assertTrue(samplePlayer2.diceInHand.dice.contains(D4))
    }

    @Test
    fun invoke_whenHandFull_doesNotDrawAndDoesNotChronicle() {
        // Arrange
        every { mockDeckManager.handSize } returns 5
        every { mockDeckManager.getItemsInSupply() } returns listOf(
            HandItem.Card(FakeCards.fakeRoot),
            HandItem.Dice(D6)
        )

        // Act
        samplePlayer.drawHand(1)

        // Assert
        verify(exactly = 0) { mockDeckManager.drawCard() }
        verify(exactly = 0) { mockDeckManager.drawDie() }
        verify(exactly = 0) { mockGameChronicle(any()) }
    }
} 
