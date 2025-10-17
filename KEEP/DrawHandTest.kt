package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.cards.di.GameCardIDsFactory
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.effect.FloralBonusCount
import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.random.RandomizerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawHandTest {

    private lateinit var deckManager: DeckManager
    private val mockDeckManager: DeckManager = mockk(relaxed = true)
    private lateinit var floralBonusCount: FloralBonusCount
    private lateinit var buddingStack: BuddingStack
    private lateinit var cardManager: CardManager
    private val mockDecisionDirector: DecisionDirector = mockk(relaxed = true)
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var D6: Die
    private lateinit var D4: Die
    private lateinit var D8: Die
    private lateinit var startingDice: List<Die>
    private lateinit var gameCardIDsFactory: GameCardIDsFactory
    private val mockGameChronicle: GameChronicle = mockk(relaxed = true)
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
        floralBonusCount = FloralBonusCount()
        buddingStack = BuddingStack(cardManager, gameCardIDsFactory)
        D4 = dieFactory(DieSides.D4)
        D6 = dieFactory(DieSides.D6)
        D8 = dieFactory(DieSides.D8)
        startingDice = dieFactory.startingDice

        samplePlayer = Player(
            mockDeckManager,
            buddingStack,
            floralBonusCount,
            cardManager,
            dieFactory,
            costScore,
            mockDecisionDirector
        )
        samplePlayer2 = Player(
            deckManager,
            buddingStack,
            floralBonusCount,
            cardManager,
            dieFactory,
            costScore,
            mockDecisionDirector
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
        samplePlayer2.addCardToBed(FakeCards.fakeRoot2.id)
        samplePlayer2.addDieToBed(D4)

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
        samplePlayer2.addCardToBed(FakeCards.fakeRoot2.id)
        samplePlayer2.addCardToBed(FakeCards.fakeVine.id)
        samplePlayer2.addDieToBed(D4)
        samplePlayer2.addDieToBed(D8)

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
        samplePlayer2.addCardToBed(FakeCards.fakeBloom.id)
        samplePlayer2.addCardToBed(FakeCards.fakeVine.id)
        samplePlayer2.addDieToBed(D4)
        samplePlayer2.addDieToBed(D8)

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
        samplePlayer2.addCardToBed(FakeCards.fakeRoot.id)
        samplePlayer2.addCardToBed(FakeCards.fakeRoot2.id)
        samplePlayer2.addCardToBed(FakeCards.fakeBloom.id)
        samplePlayer2.addCardToBed(FakeCards.fakeVine.id)
        samplePlayer2.addDieToBed(D4)
        samplePlayer2.addDieToBed(D6)
        samplePlayer2.addDieToBed(D8)

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
        samplePlayer2.addCardToBed(FakeCards.fakeBloom.id)
        samplePlayer2.addCardToBed(FakeCards.fakeVine.id)
        samplePlayer2.addDieToSupply(D4)
        samplePlayer2.addDieToSupply(D6)
        samplePlayer2.addDieToBed(D8)

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
            HandItem.aCard(FakeCards.fakeRoot),
            HandItem.aDie(D6)
        )

        // Act
        samplePlayer.drawHand(1)

        // Assert
        verify(exactly = 0) { mockDeckManager.drawCard() }
        verify(exactly = 0) { mockDeckManager.drawDie() }
        verify(exactly = 0) { mockGameChronicle(any()) }
    }

    @Test
    fun invoke_whenSupplyLimitedAndPreferredCardCountLow_usesAllAvailableSupply() {
        // Arrange
        samplePlayer2.addCardToSupply(FakeCards.fakeRoot.id)
        samplePlayer2.addCardToSupply(FakeCards.fakeRoot2.id)
        samplePlayer2.addCardToSupply(FakeCards.fakeBloom.id)
        samplePlayer2.addDieToSupply(D6)

        // Act
        samplePlayer2.drawHand(1) // Try to draw 1 card and rest dice, but supply has 3 cards and 1 die

        // Assert
        assertEquals(3, samplePlayer2.cardsInHand.size)
        assertTrue(samplePlayer2.cardsInHand.contains(FakeCards.fakeRoot))
        assertTrue(samplePlayer2.cardsInHand.contains(FakeCards.fakeRoot2))
        assertTrue(samplePlayer2.cardsInHand.contains(FakeCards.fakeBloom))
        assertEquals(1, samplePlayer2.diceInHand.size)
        assertEquals(D6, samplePlayer2.diceInHand.dice[0])
    }
} 
