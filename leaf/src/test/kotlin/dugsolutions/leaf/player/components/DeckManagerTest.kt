package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.tool.Randomizer
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeckManagerTest {
    companion object {
        // Card IDs
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
    }

    private lateinit var supply: StackManager
    private lateinit var hand: StackManager
    private lateinit var compost: StackManager
    private lateinit var deckManager: DeckManager
    private lateinit var randomizer: Randomizer
    private lateinit var dieFactory: DieFactory

    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die

    @BeforeEach
    fun setup() {
        // Create mock stack managers
        supply = mockk(relaxed = true)
        hand = mockk(relaxed = true)
        compost = mockk(relaxed = true)
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)

        // Create test dice
        d4 = dieFactory(DieSides.D4)
        d6 = dieFactory(DieSides.D6)
        d8 = dieFactory(DieSides.D8)

        // Initialize deck manager
        deckManager = DeckManager(supply, hand, compost, dieFactory)

        every { compost.addCard(any()) } returns true
        every { compost.addDie(any()) } returns true
        every { hand.clear() } just Runs
        every { supply.clear() } just Runs
        every { compost.clear() } just Runs
        every { hand.addCard(any()) } returns true
        every { supply.addCard(any()) } returns true
        every { compost.addCard(any()) } returns true
    }

    // Properties tests
    @Test
    fun handSize_returnsCorrectTotal() {
        // Arrange
        every { hand.cardCount } returns 2
        every { hand.diceCount } returns 3

        // Act
        val result = deckManager.handSize

        // Assert
        assertEquals(5, result)
    }

    @Test
    fun isSupplyEmpty_whenSupplyIsEmpty_returnsTrue() {
        // Arrange
        every { supply.isEmpty } returns true

        // Act
        val result = deckManager.isSupplyEmpty

        // Assert
        assertTrue(result)
    }

    @Test
    fun isSupplyEmpty_whenSupplyHasItems_returnsFalse() {
        // Arrange
        every { supply.isEmpty } returns false

        // Act
        val result = deckManager.isSupplyEmpty

        // Assert
        assertFalse(result)
    }

    @Test
    fun pipTotal_returnsHandPipTotal() {
        // Arrange
        every { hand.pipTotal } returns 7

        // Act
        val result = deckManager.pipTotal

        // Assert
        assertEquals(7, result)
    }

    @Test
    fun bloomCount_returnsSumOfAllStacks() {
        // Arrange
        every { supply.bloomCount } returns 4
        every { hand.bloomCount } returns 1
        every { compost.bloomCount } returns 2

        // Act
        val result = deckManager.bloomCount

        // Assert
        assertEquals(7, result)
    }

    // Setup tests
    @Test
    fun setup_addsCardsAndDiceToSupply() {
        // Arrange
        val ids = listOf(CARD_ID_1, CARD_ID_2)
        val seedlings = mockk<GameCards>()
        val startingDice = listOf(d4, d6)
        every { seedlings.cardIds } returns ids
        every { supply.addAllCards(ids) } just Runs
        every { supply.addAllDice(startingDice) } just Runs

        // Act
        deckManager.setup(seedlings, startingDice)

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
        val result = deckManager.hasCardInHand(CARD_ID_1)

        // Assert
        assertTrue(result)
    }

    @Test
    fun hasDieInHand_whenDieExists_returnsTrue() {
        // Arrange
        every { hand.hasDie(d6) } returns true

        // Act
        val result = deckManager.hasDieInHand(d6)

        // Assert
        assertTrue(result)
    }

    @Test
    fun getItemsInHand_returnsHandItems() {
        // Arrange
        val items = listOf(
            HandItem.Card(mockk { every { id } returns CARD_ID_1 }),
            HandItem.Dice(d6)
        )
        every { hand.getItems() } returns items

        // Act
        val result = deckManager.getItemsInHand()

        // Assert
        assertEquals(items, result)
    }

    // Discard tests
    @Test
    fun discard_whenCardExists_movesToCompost() {
        // Arrange
        every { hand.hasCard(CARD_ID_1) } returns true
        every { hand.removeCard(CARD_ID_1) } returns true
        every { compost.addCard(CARD_ID_1) } returns true

        // Act
        val result = deckManager.discard(CARD_ID_1)

        // Assert
        assertTrue(result)
        verify { hand.removeCard(CARD_ID_1) }
        verify { compost.addCard(CARD_ID_1) }
    }

    @Test
    fun discard_whenDieExists_movesToCompost() {
        // Arrange
        every { hand.hasDie(d6) } returns true
        every { hand.removeDie(d6) } returns true
        every { compost.addDie(d6) } returns true

        // Act
        val result = deckManager.discard(d6)

        // Assert
        assertTrue(result)
        verify { hand.removeDie(d6) }
        verify { compost.addDie(d6) }
    }

    @Test
    fun discard_whenDieValueExists_movesToCompost() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { hand.hasDie(dieValue) } returns true
        every { hand.removeDie(dieValue) } returns true
        every { compost.addDie(die) } returns true

        // Act
        val result = deckManager.discard(dieValue)

        // Assert
        assertTrue(result)
        verify { hand.removeDie(dieValue) }
        verify { compost.addDie(die) }
    }

    @Test
    fun discard_whenDieValueDoesNotExist_returnsFalse() {
        // Arrange
        val dieValue = DieValue(6, 4)
        every { hand.hasDie(dieValue) } returns false

        // Act
        val result = deckManager.discard(dieValue)

        // Assert
        assertFalse(result)
        verify(exactly = 0) { hand.removeDie(any<DieValue>()) }
        verify(exactly = 0) { compost.addDie(any()) }
    }

    // Drawing tests
    @Test
    fun drawCard_whenSupplyHasCard_addsToHand() {
        // Arrange
        every { supply.drawCard() } returns CARD_ID_1
        every { hand.addCard(CARD_ID_1) } returns true

        // Act
        val result = deckManager.drawCard()

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
        val result = deckManager.drawDie()

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
        val result = deckManager.drawBestDie()

        // Assert
        assertEquals(d8, result)
        verify { supply.drawHighestDie() }
        verify { hand.addDie(d8) }
    }

    // Resource cycling tests
    @Test
    fun resupply_movesAllItemsFromCompostToSupply() {
        // Arrange
        val items = listOf(
            HandItem.Card(mockk { every { id } returns CARD_ID_1 }),
            HandItem.Dice(d6),
            HandItem.Card(mockk { every { id } returns CARD_ID_2 })
        )
        every { compost.getItems() } returns items
        every { supply.addAllCards(any()) } just Runs
        every { supply.addAllDice(any()) } just Runs

        // Act
        deckManager.resupply()

        // Assert
        verify { supply.addAllCards(listOf(CARD_ID_1, CARD_ID_2)) }
        verify { supply.addAllDice(listOf(d6)) }
        verify { compost.clear() }
    }

    @Test
    fun discardHand_movesAllItemsToCompost() {
        // Arrange
        val items = listOf(
            HandItem.Card(mockk { every { id } returns CARD_ID_1 }),
            HandItem.Dice(d6),
            HandItem.Card(mockk { every { id } returns CARD_ID_2 })
        )
        every { hand.getItems() } returns items

        // Act
        deckManager.discardHand()

        // Assert
        verify { compost.addCard(CARD_ID_1) }
        verify { compost.addDie(d6) }
        verify { compost.addCard(CARD_ID_2) }
        verify { hand.clear() }
    }

    @Test
    fun clear_clearsAllStacks() {
        // Act
        deckManager.clear()

        // Assert
        verify { supply.clear() }
        verify { hand.clear() }
        verify { compost.clear() }
    }

    @Test
    fun addCardToSupply_whenSuccessful_returnsTrue() {
        // Arrange
        every { supply.addCard(CARD_ID_1) } returns true

        // Act
        val result = deckManager.addCardToSupply(CARD_ID_1)

        // Assert
        assertTrue(result)
        verify { supply.addCard(CARD_ID_1) }
    }

    @Test
    fun addCardToSupply_whenUnsuccessful_returnsFalse() {
        // Arrange
        every { supply.addCard(CARD_ID_1) } returns false

        // Act
        val result = deckManager.addCardToSupply(CARD_ID_1)

        // Assert
        assertFalse(result)
        verify { supply.addCard(CARD_ID_1) }
    }

    @Test
    fun addDieToSupply_whenSuccessful_returnsTrue() {
        // Arrange
        every { supply.addDie(d6) } returns true

        // Act
        val result = deckManager.addDieToSupply(d6)

        // Assert
        assertTrue(result)
        verify { supply.addDie(d6) }
    }

    @Test
    fun addDieToSupply_whenUnsuccessful_returnsFalse() {
        // Arrange
        every { supply.addDie(d6) } returns false

        // Act
        val result = deckManager.addDieToSupply(d6)

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
        val result = deckManager.addDieToSupply(dieValue)

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
        val result = deckManager.addDieToSupply(dieValue)

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
        val result = deckManager.addDieToHand(dieValue)

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
        val result = deckManager.addDieToHand(dieValue)

        // Assert
        assertFalse(result)
        verify { hand.addDie(die) }
    }

    @Test
    fun addDieToCompost_whenDieValueSuccessful_returnsTrue() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { compost.addDie(die) } returns true

        // Act
        val result = deckManager.addDieToCompost(dieValue)

        // Assert
        assertTrue(result)
        verify { compost.addDie(die) }
    }

    @Test
    fun addDieToCompost_whenDieValueUnsuccessful_returnsFalse() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        every { compost.addDie(die) } returns false

        // Act
        val result = deckManager.addDieToCompost(dieValue)

        // Assert
        assertFalse(result)
        verify { compost.addDie(die) }
    }

    @Test
    fun trashSeedlingCards_trashesCardsFromAllStacks() {
        // Act
        deckManager.trashSeedlingCards()

        // Assert
        verify { supply.trashSeedlingCards() }
        verify { hand.trashSeedlingCards() }
        verify { compost.trashSeedlingCards() }
    }
} 
