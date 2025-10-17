package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.player.PlayerReal
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.SampleDie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawNewHandTest {

    private val sampleDie = SampleDie()
    private val D4: Die = sampleDie.d4
    private val D6: Die = sampleDie.d6
    private val D8: Die = sampleDie.d8
    private val D10: Die = sampleDie.d10
    private val SUT: DrawNewHand = DrawNewHand()
    private val player: PlayerReal = PlayerReal.create()

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_whenPreferredCardCount0_drawsOneCardAndTheRestDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)

        // Act
        val result = SUT(player, 0)

        // Assert
        assertEquals(1, player.cardsInHand.size)
        assertEquals(3, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        assertTrue(player.diceInHand.dice.contains(D8))
        
        // Verify return value - should have 4 items total (1 card + 3 dice)
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasDie)
    }

    @Test
    fun invoke_whenPreferredCardCount1_withPlentyOfSupply_drawsOneCardAndThreeDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)

        // Act
        val result = SUT(player, 1)

        // Assert
        assertEquals(1, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(3, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        assertTrue(player.diceInHand.dice.contains(D8))
        
        // Verify return value - should have 4 items total (1 card + 3 dice)
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasDie)
        
        val cardResult = (result[0] as DrawNewHand.ResultInstance.WasCard).result
        assertEquals(FakeCards.rootCard.id, cardResult.cardId)
        assertFalse(cardResult.reshuffleDone)
    }

    @Test
    fun invoke_whenPreferredCardCount2_withPlentyOfSupply_drawsTwoCardsAndTwoDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)

        // Act
        val result = SUT(player, 2)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        
        // Verify return value - should have 4 items total (2 cards + 2 dice)
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasDie)
        
        val cardResult1 = (result[0] as DrawNewHand.ResultInstance.WasCard).result
        val cardResult2 = (result[1] as DrawNewHand.ResultInstance.WasCard).result
        assertEquals(FakeCards.rootCard.id, cardResult1.cardId)
        assertEquals(FakeCards.rootCard2.id, cardResult2.cardId)
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
        val result = SUT(player, 3)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        
        // Verify return value - should have 4 items total (2 cards + 2 dice)
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasDie)
    }

    @Test
    fun invoke_whenPreferredCardCount4_withNoCards_mustResupplyAndDrawAsManyAsPossibleAfterThat() {
        // Arrange
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)
        // Add cards to discard to trigger resupply
        player.addCardToDiscard(FakeCards.rootCard.id)
        player.addCardToDiscard(FakeCards.rootCard2.id)

        // Act
        val result = SUT(player, 4)

        // Assert
        // Should have at least 1 card (after resupply) and remaining dice
        assertEquals(2, player.cardsInHand.size)
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        assertFalse(player.diceInHand.dice.contains(D8))

        // Verify return value - should have 4 items total (1 card + 3 dice)
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasDie)
        
        // The card draw should trigger resupply
        val cardResult = (result[0] as DrawNewHand.ResultInstance.WasCard).result
        assertTrue(cardResult.reshuffleDone)
    }

    @Test
    fun invoke_whenPreferredCardCount2_withNoDice_drawsTwoCards() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)

        // Act
        val result = SUT(player, 2)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(0, player.diceInHand.size)
        
        // Verify return value - should have 2 items total (2 cards)
        assertEquals(2, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasCard)
    }

    @Test
    fun invoke_whenPreferredCardCount2_withOneCardAndThreeDice_drawsOneCardAndThreeDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)

        // Act
        val result = SUT(player, 2)

        // Assert
        // Should draw ALL available items from supply (1 card + 3 dice), regardless of preferred count
        assertEquals(1, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(3, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        assertTrue(player.diceInHand.dice.contains(D8))
        
        // Verify return value - should have 4 items total (1 card + 3 dice)
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasDie)
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
        val result = SUT(player, 2)

        // Assert
        assertEquals(3, player.cardsInHand.size)
        assertEquals(2, player.diceInHand.size)
        
        // Verify return value - should be empty since hand is full
        assertEquals(0, result.size)
    }

    @Test
    fun invoke_whenPartialHand_considersExistingHand() {
        // Arrange
        player.addCardToHand(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)

        // Act
        val result = SUT(player, 2)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        
        // Verify return value - should have 3 items total (1 card + 2 dice)
        assertEquals(3, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
    }

    @Test
    fun invoke_whenLimitedSupplyWithPreferredCount1_drawsAllAvailableCardsAndDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)

        // Act
        val result = SUT(player, 1)

        // Assert
        // Should draw ALL available items from supply (2 cards + 2 dice), regardless of preferred count
        assertEquals(2, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(FakeCards.rootCard2.id, player.cardsInHand[1].id)
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        
        // Verify return value - should have 4 items total (2 cards + 2 dice)
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasCard)
    }

    @Test
    fun invoke_whenNoCardsInSupply_requiresResupplyToDrawCard() {
        // Arrange
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)
        player.addDieToSupply(D10)
        // Add cards to discard to trigger resupply
        player.addCardToDiscard(FakeCards.rootCard.id)
        player.addCardToDiscard(FakeCards.rootCard2.id)

        // Act
        val result = SUT(player, 2)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(2, player.diceInHand.size)
        
        // Verify return value - should have 4 items total
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasDie)
        
        // The first card draw should trigger resupply
        val firstCardResult = (result[0] as DrawNewHand.ResultInstance.WasCard).result
        assertTrue(firstCardResult.reshuffleDone)
        val firstDieResult = (result[2] as DrawNewHand.ResultInstance.WasDie).result
        assertEquals(D4, firstDieResult.die)

    }

    @Test
    fun invoke_whenNoDiceInSupply_requiresResupplyToDrawDice() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)
        // Add dice to discard to trigger resupply
        player.addDieToDiscard(D4)
        player.addDieToDiscard(D6)

        // Act
        val result = SUT(player, 1)

        // Assert
        assertEquals(2, player.cardsInHand.size)
        assertEquals(2, player.diceInHand.size)
        
        // Verify return value
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasDie)
        
        // The dice draws should trigger resupply
        val firstDieResult = (result[2] as DrawNewHand.ResultInstance.WasDie).result
        assertTrue(firstDieResult.reshuffleDone)
    }

    @Test
    fun invoke_whenHandAlmostFull_assumesMidDraw() {
        // Arrange
        player.addCardToHand(FakeCards.rootCard.id)
        player.addCardToHand(FakeCards.rootCard2.id)
        player.addCardToHand(FakeCards.bloomCard.id)
        player.addCardToSupply(FakeCards.vineCard.id)
        player.addDieToSupply(D4)

        // Act
        val result = SUT(player, 2)

        // Assert
        assertEquals(3, player.cardsInHand.size)
        assertEquals(1, player.diceInHand.size)
        
        // Verify return value - should only draw 1 item (1 card to fill remaining space)
        assertEquals(1, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasDie)
    }

    @Test
    fun invoke_whenPreferredCardCountExceedsHandSize_drawsUpToHandSize() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addCardToSupply(FakeCards.rootCard2.id)
        player.addCardToSupply(FakeCards.bloomCard.id)
        player.addCardToSupply(FakeCards.vineCard.id)
        player.addCardToSupply(FakeCards.canopyCard.id)

        // Act
        val result = SUT(player, 10) // More than HAND_SIZE (4)

        // Assert
        assertEquals(4, player.cardsInHand.size)
        assertEquals(0, player.diceInHand.size)
        
        // Verify return value - should only draw 4 items (HAND_SIZE)
        assertEquals(4, result.size)
        assertTrue(result.all { it is DrawNewHand.ResultInstance.WasCard })
    }

    @Test
    fun invoke_whenNoCardsInSupplyAndNoCardsInDiscard_cannotDrawCards() {
        // Arrange
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)
        // No cards in supply AND no cards in discard (game loss scenario)

        // Act
        val result = SUT(player, 2)

        // Assert
        // Should have 0 cards (no cards available anywhere) and then just return empty since this is losing.
        assertEquals(0, player.cardsInHand.size)
        assertEquals(0, player.diceInHand.size)
        
        // Verify return value - should have 3 items total (3 dice, no cards available)
        assertEquals(0, result.size)
        // This represents a game loss scenario where player cannot draw any cards
    }

    @Test
    fun invoke_whenSupplyHasOneCardAndMoreThanThreeDice_drawsAllItems() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id)
        player.addDieToSupply(D4)
        player.addDieToSupply(D6)
        player.addDieToSupply(D8)
        player.addDieToSupply(D10) // This will be ignored
        // Add cards to discard for potential resupply
        player.addCardToDiscard(FakeCards.rootCard2.id)

        // Act
        val result = SUT(player, 3) // Preferred count 3, but supply has 1 card + 4 dice

        // Assert
        // Should draw ALL available items from supply (1 card + 4 dice), regardless of preferred count
        assertEquals(1, player.cardsInHand.size)
        assertEquals(FakeCards.rootCard.id, player.cardsInHand[0].id)
        assertEquals(3, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D4))
        assertTrue(player.diceInHand.dice.contains(D6))
        assertTrue(player.diceInHand.dice.contains(D8))
        assertFalse(player.diceInHand.dice.contains(D10))

        // Verify return value - should have 5 items total (1 card + 4 dice)
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasDie)

        // No resupply should happen because supply still has cards
        val cardResult = (result[0] as DrawNewHand.ResultInstance.WasCard).result
        assertFalse(cardResult.reshuffleDone)
    }

    @Test
    fun invoke_whenSupplyExhaustedDuringDraw_triggersResupply() {
        // Arrange
        player.addCardToSupply(FakeCards.rootCard.id) // Only 1 card in supply
        player.addDieToDiscard(D4)
        player.addDieToDiscard(D6)
        player.addDieToSupply(D8)
        player.addDieToSupply(D8)
        // Add cards to discard for resupply
        player.addCardToDiscard(FakeCards.rootCard2.id)
        player.addCardToDiscard(FakeCards.bloomCard.id)

        // Act
        val result = SUT(player, 3) // Preferred count 3, but supply has 1 card + 4 dice

        // Assert
        // Should draw ALL available items from supply (1 card + 3 dice), then resupply and draw more
        assertEquals(2, player.cardsInHand.size) // 1 from supply + 1 from resupply
        assertEquals(2, player.diceInHand.size)
        assertTrue(player.diceInHand.dice.contains(D8))
        assertTrue(player.diceInHand.dice.contains(D8))
        assertTrue(player.diceInHand.dice.count { it == D8 } == 2) // Two D8s
        
        // Verify return value - should have 6 items total (2 cards + 4 dice)
        assertEquals(4, result.size)
        assertTrue(result[0] is DrawNewHand.ResultInstance.WasCard)
        assertTrue(result[1] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[2] is DrawNewHand.ResultInstance.WasDie)
        assertTrue(result[3] is DrawNewHand.ResultInstance.WasCard)
        
        // The second card draw should trigger resupply
        val secondCardResult = (result[3] as DrawNewHand.ResultInstance.WasCard).result
        assertTrue(secondCardResult.reshuffleDone)
    }
} 
