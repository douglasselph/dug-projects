package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.random.RandomizerTD
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.SampleDie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawNewHandTest {

    private lateinit var cardManager: CardManager
    private val sampleDie = SampleDie()
    private val D4: Die = sampleDie.d4
    private val D6: Die = sampleDie.d6
    private val D8: Die = sampleDie.d8
    private val SUT: DrawNewHand = DrawNewHand()
    private lateinit var player: PlayerTD

    @BeforeEach
    fun setup() {
        val randomizer = RandomizerTD()
        val costScore = CostScore()
        val gameCardsFactory = GameCardsFactory(randomizer, costScore)
        cardManager = CardManager(gameCardsFactory)
        cardManager.loadCards(FakeCards.ALL_CARDS)
        player = PlayerTD("Test Player", 1, cardManager)
    }

    @Test
    fun invoke_whenPreferredCardCount0_drawsOneCardAndTheRestDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)

        // Act
        SUT(player, 0)

        // Assert
        assertEquals(1, player.cardsInHand.size)
        assertEquals(3, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        assertTrue(player.diceInHand.dice.contains(D8))
    }

    @Test
    fun invoke_whenPreferredCardCount1_withPlentyOfSupply_drawsOneCardAndThreeDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)

        // Act
        SUT(player, 1)

        // Assert
        assertEquals(1, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(3, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        assertTrue(player.diceInHand.dice.contains(D8))
    }

    @Test
    fun invoke_whenPreferredCardCount2_withPlentyOfSupply_drawsTwoCardsAndTwoDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)

        // Act
        SUT(player, 2)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
    }

    @Test
    fun invoke_whenPreferredCardCount3_withLimitedCards_drawsAllCardsAndRemainingDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)

        // Act
        SUT(player, 3)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
    }

    @Test
    fun invoke_whenPreferredCardCount4_withNoCards_drawsAllDice() {
        // Arrange
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)

        // Act
        SUT(player, 4)

        // Assert
        assertEquals(0, player.cardsInHand.size)
        assertEquals(3, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        assertTrue(player.diceInHand.dice.contains(D8))
    }

    @Test
    fun invoke_whenPreferredCardCount2_withNoDice_drawsTwoCards() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)

        // Act
        SUT(player, 2)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(0, player.diceInHand.size)
    }

    @Test
    fun invoke_whenPreferredCardCount2_withOneCardAndThreeDice_drawsOneCardAndThreeDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)

        // Act
        SUT(player, 2)

        // Assert
        assertEquals(1, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(3, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        assertTrue(player.diceInHand.dice.contains(D8))
    }

    @Test
    fun invoke_whenHandFull_doesNotDraw() {
        // Arrange
        player.addCardToHand(FakeCards.rootCard.id)
        player.addCardToHand(FakeCards.rootCard2.id)
        player.addCardToHand(FakeCards.bloomCard.id)
        player.addDieToHand(D4)
        player.addDieToHand(D6)

        // Act
        SUT(player, 2)

        // Assert
        assertEquals(3, player.cardsInHand.size)
        assertEquals(2, player.diceInHand.size)
    }

    @Test
    fun invoke_whenPartialHand_considersExistingHand() {
        // Arrange
        player.addCardToHand(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)

        // Act
        SUT(player, 2)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
    }

    @Test
    fun invoke_whenLimitedSupplyWithPreferredCount1_drawsAllAvailableCardsAndDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)

        // Act
        SUT(player, 1)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
    }
} 
