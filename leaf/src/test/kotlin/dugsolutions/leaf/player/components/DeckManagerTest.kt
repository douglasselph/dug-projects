package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.Randomizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeckManagerTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
    }
    private lateinit var supply: StackManager
    private lateinit var hand: StackManager
    private lateinit var discardPatch: StackManager
    private lateinit var randomizer: Randomizer
    private lateinit var dieFactory: DieFactory

    private lateinit var SUT: DeckManager

    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die

    @BeforeEach
    fun setup() {
        supply = mockk(relaxed = true)
        hand = mockk(relaxed = true)
        discardPatch = mockk(relaxed = true)
        randomizer = Randomizer.create()
        dieFactory = DieFactory(randomizer)

        d4 = dieFactory(DieSides.D4)
        d6 = dieFactory(DieSides.D6)
        d8 = dieFactory(DieSides.D8)

        SUT = DeckManager(supply, hand, discardPatch, dieFactory)

        every { discardPatch.addCard(any()) } returns true
        every { discardPatch.addDie(any()) } returns true
        every { hand.addCard(any()) } returns true
        every { supply.addCard(any()) } returns true
        every { discardPatch.addCard(any()) } returns true
    }

    @Test
    fun handSize_returnsCorrectTotal() {
        // Arrange
        every { hand.cardCount } returns 2
        every { hand.diceCount } returns 3

        // Act
        val result = SUT.handSize

        // Assert
        assertEquals(5, result)
    }

    @Test
    fun isResupplyNeeded_whenSupplyHasNoCards_returnsTrue() {
        // Arrange
        every { supply.cardCount } returns 0

        // Act
        val result = SUT.isResupplyNeeded

        // Assert
        assertTrue(result)
    }

    @Test
    fun isResupplyNeeded_whenSupplyHasCards_returnsFalse() {
        // Arrange
        every { supply.cardCount } returns 3

        // Act
        val result = SUT.isResupplyNeeded

        // Assert
        assertFalse(result)
    }

    @Test
    fun isResupplyNeeded_whenSupplyHasDiceButNoCards_returnsTrue() {
        // Arrange
        every { supply.cardCount } returns 0
        every { supply.diceCount } returns 5

        // Act
        val result = SUT.isResupplyNeeded

        // Assert
        assertTrue(result)
    }

    @Test
    fun pipTotal_returnsHandPipTotal() {
        // Arrange
        every { hand.pipTotal } returns 7

        // Act
        val result = SUT.pipTotal

        // Assert
        assertEquals(7, result)
    }

    // Setup tests
    @Test
    fun setup_addsCardsAndDiceToSupply() {
        // Arrange
        val ids = listOf(CARD_ID_1, CARD_ID_2)
        val seedlings = mockk<GameCards>(relaxed = true)
        val startingDice = listOf(d4, d6)
        every { seedlings.cardIds } returns ids

        // Act
        SUT.setup(seedlings, startingDice)

        // Assert
        verify { supply.addAllCards(ids) }
        verify { supply.addAllDice(startingDice) }
    }

    // Hand management tests
    @Test
    fun hasCardInHand_whenCardExists_returnsTrue() {
        // Arrange
        every { hand.hasCard(CARD_ID_1) } returns true

        // Act
        val result = SUT.hasCardInHand(CARD_ID_1)

        // Assert
        assertTrue(result)
    }

    @Test
    fun hasDieInHand_whenDieExists_returnsTrue() {
        // Arrange
        every { hand.hasDie(d6) } returns true

        // Act
        val result = SUT.hasDieInHand(d6)

        // Assert
        assertTrue(result)
    }

    @Test
    fun getItemsInHand_returnsHandItems() {
        // Arrange
        val mockCard = mockk<GameCard> { every { id } returns CARD_ID_1 }
        val items = listOf(
            HandItem.aCard(mockCard),
            HandItem.aDie(d6)
        )
        every { hand.getItems() } returns items

        // Act
        val result = SUT.getItemsInHand()

        // Assert
        assertEquals(items, result)
    }

    @Test
    fun discard_whenCardExists_movesToDiscard() {
        // Arrange
        every { hand.hasCard(CARD_ID_1) } returns true
        every { hand.removeCard(CARD_ID_1) } returns true
        every { discardPatch.addCard(CARD_ID_1) } returns true

        // Act
        val result = SUT.discard(CARD_ID_1)

        // Assert
        assertTrue(result)
        verify { hand.removeCard(CARD_ID_1) }
        verify { discardPatch.addCard(CARD_ID_1) }
    }

    @Test
    fun discard_whenDieExists_movesToDiscard() {
        // Arrange
        every { hand.hasDie(d6) } returns true
        every { hand.removeDie(d6) } returns true
        every { discardPatch.addDie(d6) } returns true

        // Act
        val result = SUT.discard(d6)

        // Assert
        assertTrue(result)
        verify { hand.removeDie(d6) }
        verify { discardPatch.addDie(d6) }
    }

    @Test
    fun discard_whenDieValueExists_movesToDiscard() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { hand.hasDie(dieValue) } returns true
        every { hand.removeDie(dieValue) } returns true
        every { discardPatch.addDie(die) } returns true

        // Act
        val result = SUT.discard(dieValue)

        // Assert
        assertTrue(result)
        verify { hand.removeDie(dieValue) }
        verify { discardPatch.addDie(die) }
    }

    @Test
    fun discard_whenDieValueDoesNotExist_returnsFalse() {
        // Arrange
        val dieValue = DieValue(6, 4)
        every { hand.hasDie(dieValue) } returns false

        // Act
        val result = SUT.discard(dieValue)

        // Assert
        assertFalse(result)
        verify(exactly = 0) { hand.removeDie(any<DieValue>()) }
        verify(exactly = 0) { discardPatch.addDie(any()) }
    }

    @Test
    fun drawCard_whenSupplyHasCard_addsToHand() {
        // Arrange
        every { supply.drawCard() } returns CARD_ID_1
        every { hand.addCard(CARD_ID_1) } returns true

        // Act
        val result = SUT.drawCard()

        // Assert
        assertEquals(CARD_ID_1, result)
        verify { supply.drawCard() }
        verify { hand.addCard(CARD_ID_1) }
    }

    @Test
    fun drawDie_whenSupplyHasDie_addsToHand() {
        // Arrange
        every { supply.drawLowestDie() } returns d6
        every { hand.addDie(d6) } returns true

        // Act
        val result = SUT.drawDie()

        // Assert
        assertEquals(d6, result)
        verify { supply.drawLowestDie() }
        verify { hand.addDie(d6) }
    }

    @Test
    fun drawBestDie_whenSupplyHasDie_addsToHand() {
        // Arrange
        every { supply.drawHighestDie() } returns d8
        every { hand.addDie(d8) } returns true

        // Act
        val result = SUT.drawBestDie()

        // Assert
        assertEquals(d8, result)
        verify { supply.drawHighestDie() }
        verify { hand.addDie(d8) }
    }

    @Test
    fun resupply_movesAllItemsFromDiscardToSupply() {
        // Arrange
        val mockCard1 = mockk<GameCard> { every { id } returns CARD_ID_1 }
        val mockCard2 = mockk<GameCard> { every { id } returns CARD_ID_2 }
        val items = listOf(
            HandItem.aCard(mockCard1),
            HandItem.aDie(d6),
            HandItem.aCard(mockCard2)
        )
        every { discardPatch.getItems() } returns items

        // Act
        val result = SUT.resupply()

        // Assert
        assertTrue(result)
        verify { supply.addAllCards(listOf(CARD_ID_1, CARD_ID_2)) }
        verify { supply.addAllDice(listOf(d6)) }
        verify { discardPatch.clear() }
        verify { supply.shuffle() }
    }

    @Test
    fun resupply_whenDiscardEmpty_stillShufflesSupply() {
        // Arrange
        every { discardPatch.getItems() } returns emptyList()

        // Act
        val result = SUT.resupply()

        // Assert
        assertTrue(result)
        verify { supply.shuffle() }
    }

    @Test
    fun resupply_whenDiscardHasOnlyCards_movesOnlyCardsToSupply() {
        // Arrange
        val mockCard1 = mockk<GameCard> { every { id } returns CARD_ID_1 }
        val mockCard2 = mockk<GameCard> { every { id } returns CARD_ID_2 }
        val items = listOf(
            HandItem.aCard(mockCard1),
            HandItem.aCard(mockCard2)
        )
        every { discardPatch.getItems() } returns items

        // Act
        val result = SUT.resupply()

        // Assert
        assertTrue(result)
        verify { supply.addAllCards(listOf(CARD_ID_1, CARD_ID_2)) }
        verify { supply.addAllDice(emptyList<Die>()) }
        verify { discardPatch.clear() }
        verify { supply.shuffle() }
    }

    @Test
    fun resupply_whenDiscardHasOnlyDice_movesOnlyDiceToSupply() {
        // Arrange
        val items = listOf(
            HandItem.aDie(d4),
            HandItem.aDie(d6),
            HandItem.aDie(d8)
        )
        every { discardPatch.getItems() } returns items

        // Act
        val result = SUT.resupply()

        // Assert
        assertTrue(result)
        verify { supply.addAllCards(emptyList<CardID>()) }
        verify { supply.addAllDice(listOf(d4, d6, d8)) }
        verify { discardPatch.clear() }
        verify { supply.shuffle() }
    }

    @Test
    fun discardHand_movesAllItemsToDiscard() {
        // Arrange
        val mockCard1 = mockk<GameCard> { every { id } returns CARD_ID_1 }
        val mockCard2 = mockk<GameCard> { every { id } returns CARD_ID_2 }
        val items = listOf(
            HandItem.aCard(mockCard1),
            HandItem.aDie(d6),
            HandItem.aCard(mockCard2)
        )
        every { hand.getItems() } returns items

        // Act
        SUT.discardHand()

        // Assert
        verify { discardPatch.addCard(CARD_ID_1) }
        verify { discardPatch.addDie(d6) }
        verify { discardPatch.addCard(CARD_ID_2) }
        verify { hand.clear() }
    }

    @Test
    fun clear_clearsAllStacks() {
        // Act
        SUT.clear()

        // Assert
        verify { supply.clear() }
        verify { hand.clear() }
        verify { discardPatch.clear() }
    }

    @Test
    fun addCardToSupply_whenSuccessful_returnsTrue() {
        // Arrange
        every { supply.addCard(CARD_ID_1) } returns true

        // Act
        val result = SUT.addCardToSupply(CARD_ID_1)

        // Assert
        assertTrue(result)
        verify { supply.addCard(CARD_ID_1) }
    }

    @Test
    fun addCardToSupply_whenUnsuccessful_returnsFalse() {
        // Arrange
        every { supply.addCard(CARD_ID_1) } returns false

        // Act
        val result = SUT.addCardToSupply(CARD_ID_1)

        // Assert
        assertFalse(result)
        verify { supply.addCard(CARD_ID_1) }
    }

    @Test
    fun addDieToSupply_whenSuccessful_returnsTrue() {
        // Arrange
        every { supply.addDie(d6) } returns true

        // Act
        val result = SUT.addDieToSupply(d6)

        // Assert
        assertTrue(result)
        verify { supply.addDie(d6) }
    }

    @Test
    fun addDieToSupply_whenUnsuccessful_returnsFalse() {
        // Arrange
        every { supply.addDie(d6) } returns false

        // Act
        val result = SUT.addDieToSupply(d6)

        // Assert
        assertFalse(result)
        verify { supply.addDie(d6) }
    }

    @Test
    fun addDieToSupply_whenDieValueSuccessful_returnsTrue() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { supply.addDie(die) } returns true

        // Act
        val result = SUT.addDieToSupply(dieValue)

        // Assert
        assertTrue(result)
        verify { supply.addDie(die) }
    }

    @Test
    fun addDieToSupply_whenDieValueUnsuccessful_returnsFalse() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { supply.addDie(die) } returns false

        // Act
        val result = SUT.addDieToSupply(dieValue)

        // Assert
        assertFalse(result)
        verify { supply.addDie(die) }
    }

    @Test
    fun addDieToHand_whenDieValueSuccessful_returnsTrue() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { hand.addDie(die) } returns true

        // Act
        val result = SUT.addDieToHand(dieValue)

        // Assert
        assertTrue(result)
        verify { hand.addDie(die) }
    }

    @Test
    fun addDieToHand_whenDieValueUnsuccessful_returnsFalse() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { hand.addDie(die) } returns false

        // Act
        val result = SUT.addDieToHand(dieValue)

        // Assert
        assertFalse(result)
        verify { hand.addDie(die) }
    }

    @Test
    fun addDieToDiscard_whenDieValueSuccessful_returnsTrue() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { discardPatch.addDie(die) } returns true

        // Act
        val result = SUT.addDieToDiscard(dieValue)

        // Assert
        assertTrue(result)
        verify { discardPatch.addDie(die) }
    }

    @Test
    fun addDieToDiscard_whenDieValueUnsuccessful_returnsFalse() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { discardPatch.addDie(die) } returns false

        // Act
        val result = SUT.addDieToDiscard(dieValue)

        // Assert
        assertFalse(result)
        verify { discardPatch.addDie(die) }
    }

    @Test
    fun trashSeedlingCards_trashesCardsFromAllStacks() {
        // Act
        SUT.trashSeedlingCards()

        // Assert
        verify { supply.trashSeedlingCards() }
        verify { hand.trashSeedlingCards() }
        verify { discardPatch.trashSeedlingCards() }
    }
} 
