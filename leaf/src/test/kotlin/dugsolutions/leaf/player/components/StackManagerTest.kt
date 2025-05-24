package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.GameCardIDsFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.tool.Randomizer
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.components.die.DieValue

class StackManagerTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
        private const val CARD_ID_3 = 3
    }

    private lateinit var cardManager: CardManager
    private lateinit var gameCardIDsFactory: GameCardIDsFactory
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var sampleDie: SampleDie

    private lateinit var SUT: StackManager

    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die
    private lateinit var bloomCard: GameCard
    private lateinit var rootCard: GameCard
    private lateinit var mockBloomCard: GameCard
    private lateinit var mockRootCard: GameCard

    @BeforeEach
    fun setup() {
        // Initialize random components
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)

        // Create mock dependencies
        cardManager = mockk(relaxed = true)
        gameCardIDsFactory = GameCardIDsFactory(cardManager, randomizer)

        // Create test dice
        d4 = dieFactory(DieSides.D4)
        d6 = dieFactory(DieSides.D6)
        d8 = dieFactory(DieSides.D8)
        sampleDie = SampleDie(randomizer)

        // Create test cards
        bloomCard = mockk {
            every { id } returns CARD_ID_1
            every { type } returns FlourishType.BLOOM
        }
        rootCard = mockk {
            every { id } returns CARD_ID_2
            every { type } returns FlourishType.ROOT
        }

        // Setup card manager responses
        every { cardManager.getCard(CARD_ID_1) } returns bloomCard
        every { cardManager.getCard(CARD_ID_2) } returns rootCard
        every { cardManager.getCard(CARD_ID_3) } returns null

        // Initialize stack manager
        SUT = StackManager(cardManager, gameCardIDsFactory)

        // Set up mock cards
        mockBloomCard = mockk {
            every { id } returns CARD_ID_1
            every { type } returns FlourishType.BLOOM
        }
        mockRootCard = mockk {
            every { id } returns CARD_ID_2
            every { type } returns FlourishType.ROOT
        }
    }

    // Properties tests
    @Test
    fun cardCount_returnsCorrectCount() {
        // Arrange
        SUT.addCard(CARD_ID_1)
        SUT.addCard(CARD_ID_2)

        // Act
        val result = SUT.cardCount

        // Assert
        assertEquals(2, result)
    }

    @Test
    fun diceCount_returnsCorrectCount() {
        // Arrange
        SUT.addDie(d4)
        SUT.addDie(d6)

        // Act
        val result = SUT.diceCount

        // Assert
        assertEquals(2, result)
    }

    @Test
    fun isEmpty_whenEmpty_returnsTrue() {
        // Assert
        assertTrue(SUT.isEmpty)
    }

    @Test
    fun isEmpty_whenHasCards_returnsFalse() {
        // Arrange
        SUT.addCard(CARD_ID_1)

        // Act
        val result = SUT.isEmpty

        // Assert
        assertFalse(result)
    }

    @Test
    fun isEmpty_whenHasDice_returnsFalse() {
        // Arrange
        SUT.addDie(d4)

        // Act
        val result = SUT.isEmpty

        // Assert
        assertFalse(result)
    }

    @Test
    fun pipTotal_returnsSumOfDiceValues() {
        // Arrange
        d4.adjustTo(3)
        d6.adjustTo(4)
        SUT.addDie(d4)
        SUT.addDie(d6)

        // Act
        val result = SUT.pipTotal

        // Assert
        assertEquals(7, result)
    }

    @Test
    fun bloomCount_returnsCorrectCount() {
        // Arrange
        SUT.addCard(CARD_ID_1) // Bloom card
        SUT.addCard(CARD_ID_2) // Root card

        // Act
        val result = SUT.bloomCount

        // Assert
        assertEquals(1, result)
    }

    // Card operations tests
    @Test
    fun hasCard_whenCardExists_returnsTrue() {
        // Arrange
        SUT.addCard(CARD_ID_1)

        // Act
        val result = SUT.hasCard(CARD_ID_1)

        // Assert
        assertTrue(result)
    }

    @Test
    fun hasCard_whenCardDoesNotExist_returnsFalse() {
        // Act
        val result = SUT.hasCard(CARD_ID_1)

        // Assert
        assertFalse(result)
    }

    @Test
    fun addCard_addsCardSuccessfully() {
        // Act
        val result = SUT.addCard(CARD_ID_1)

        // Assert
        assertTrue(result)
        assertTrue(SUT.hasCard(CARD_ID_1))
    }

    @Test
    fun removeCard_whenCardExists_returnsTrue() {
        // Arrange
        SUT.addCard(CARD_ID_1)

        // Act
        val result = SUT.removeCard(CARD_ID_1)

        // Assert
        assertTrue(result)
        assertFalse(SUT.hasCard(CARD_ID_1))
    }

    @Test
    fun removeCard_whenCardDoesNotExist_returnsFalse() {
        // Act
        val result = SUT.removeCard(CARD_ID_1)

        // Assert
        assertFalse(result)
    }

    @Test
    fun drawCard_whenCardsExist_returnsCard() {
        // Arrange
        SUT.addCard(CARD_ID_1)

        // Act
        val result = SUT.drawCard()

        // Assert
        assertEquals(CARD_ID_1, result)
        assertFalse(SUT.hasCard(CARD_ID_1))
    }

    @Test
    fun drawCard_whenNoCards_returnsNull() {
        // Act
        val result = SUT.drawCard()

        // Assert
        assertNull(result)
    }

    // Dice operations tests
    @Test
    fun hasDie_whenDieExists_returnsTrue() {
        // Arrange
        val d6 = sampleDie.d6
        val d6b = sampleDie.d6

        SUT.addDie(d6)
        SUT.addDie(d6b)

        // Act
        val result = SUT.hasDie(d6)
        val result2 = SUT.hasDie(d6b)

        // Assert
        assertTrue(result)
        assertTrue(result2)
    }

    @Test
    fun hasDie_whenDieExists2_returnsTrue() {
        // Arrange
        SUT.addDie(d4)

        // Act
        val result = SUT.hasDie(d4)

        // Assert
        assertTrue(result)
    }

    @Test
    fun hasDie_whenDieDoesNotExist_returnsFalse() {
        // Act
        val result = SUT.hasDie(d4)

        // Assert
        assertFalse(result)
    }

    @Test
    fun addDie_addsDieSuccessfully() {
        // Act
        val result = SUT.addDie(d4)

        // Assert
        assertTrue(result)
        assertTrue(SUT.hasDie(d4))
    }

    @Test
    fun removeDie_whenDieExists_returnsTrue() {
        // Arrange
        SUT.addDie(d4)

        // Act
        val result = SUT.removeDie(d4)

        // Assert
        assertTrue(result)
        assertFalse(SUT.hasDie(d4))
    }

    @Test
    fun removeDie_whenDieDoesNotExist_returnsFalse() {
        // Act
        val result = SUT.removeDie(d4)

        // Assert
        assertFalse(result)
    }

    @Test
    fun drawLowestDie_whenDiceExist_returnsLowestDie() {
        // Arrange
        SUT.addDie(d8)
        SUT.addDie(d4)
        SUT.addDie(d6)

        // Act
        val result = SUT.drawLowestDie()

        // Assert
        assertEquals(d4, result)
        assertFalse(SUT.hasDie(d4))
    }

    @Test
    fun drawHighestDie_whenDiceExist_returnsHighestDie() {
        // Arrange
        SUT.addDie(d4)
        SUT.addDie(d8)
        SUT.addDie(d6)

        // Act
        val result = SUT.drawHighestDie()

        // Assert
        assertEquals(d8, result)
        assertFalse(SUT.hasDie(d8))
    }

    @Test
    fun hasDie_whenDieValueExists_returnsTrue() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        SUT.addDie(die)

        // Act
        val result = SUT.hasDie(dieValue)

        // Assert
        assertTrue(result)
    }

    @Test
    fun hasDie_whenDieValueDoesNotExist_returnsFalse() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(3) // Different value
        SUT.addDie(die)

        // Act
        val result = SUT.hasDie(dieValue)

        // Assert
        assertFalse(result)
    }

    @Test
    fun hasDie_whenDieValueExistsWithDifferentSides_returnsFalse() {
        // Arrange
        val dieValue = DieValue(8, 4) // D8 with value 4
        val die = dieFactory(DieSides.D6).adjustTo(4) // D6 with value 4
        SUT.addDie(die)

        // Act
        val result = SUT.hasDie(dieValue)

        // Assert
        assertFalse(result)
    }

    @Test
    fun removeDie_whenDieValueExists_returnsTrue() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(4)
        SUT.addDie(die)

        // Act
        val result = SUT.removeDie(dieValue)

        // Assert
        assertTrue(result)
        assertFalse(SUT.hasDie(die))
    }

    @Test
    fun removeDie_whenDieValueDoesNotExist_returnsFalse() {
        // Arrange
        val dieValue = DieValue(6, 4)
        val die = dieFactory(DieSides.D6).adjustTo(3) // Different value
        SUT.addDie(die)

        // Act
        val result = SUT.removeDie(dieValue)

        // Assert
        assertFalse(result)
        assertTrue(SUT.hasDie(die)) // Original die should still be there
    }

    @Test
    fun removeDie_whenDieValueExistsWithDifferentSides_returnsFalse() {
        // Arrange
        val dieValue = DieValue(8, 4) // D8 with value 4
        val die = dieFactory(DieSides.D6).adjustTo(4) // D6 with value 4
        SUT.addDie(die)

        // Act
        val result = SUT.removeDie(dieValue)

        // Assert
        assertFalse(result)
        assertTrue(SUT.hasDie(die)) // Original die should still be there
    }

    // Bulk operations tests
    @Test
    fun addAllCards_addsAllCards() {
        // Arrange
        val cardIds = listOf(CARD_ID_1, CARD_ID_2)

        // Act
        SUT.addAllCards(cardIds)

        // Assert
        assertEquals(2, SUT.cardCount)
        assertTrue(SUT.hasCard(CARD_ID_1))
        assertTrue(SUT.hasCard(CARD_ID_2))
    }

    @Test
    fun addAllDice_addsAllDice() {
        // Arrange
        val diceList = listOf(d4, d6, d8)

        // Act
        SUT.addAllDice(diceList)

        // Assert
        assertEquals(3, SUT.diceCount)
        assertTrue(SUT.hasDie(d4))
        assertTrue(SUT.hasDie(d6))
        assertTrue(SUT.hasDie(d8))
    }

    @Test
    fun getItems_returnsAllItems() {
        // Arrange
        SUT.addCard(CARD_ID_1)
        SUT.addDie(d4)

        // Act
        val result = SUT.getItems()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it is HandItem.Card && it.card.id == CARD_ID_1 })
        assertTrue(result.any { it is HandItem.Dice && it.die == d4 })
    }

    @Test
    fun clear_removesAllItems() {
        // Arrange
        SUT.addCard(CARD_ID_1)
        SUT.addDie(d4)

        // Act
        SUT.clear()

        // Assert
        assertEquals(0, SUT.cardCount)
        assertEquals(0, SUT.diceCount)
    }

    @Test
    fun trashSeedlingCards_whenSeedlingCardsExist_removesThem() {
        // Arrange
        val seedlingCardId = 10
        val seedlingCard = mockk<GameCard> {
            every { id } returns seedlingCardId
            every { type } returns FlourishType.SEEDLING
        }
        every { cardManager.getCard(seedlingCardId) } returns seedlingCard
        
        SUT.addCard(CARD_ID_1) // Bloom card
        SUT.addCard(CARD_ID_2) // Root card
        SUT.addCard(seedlingCardId) // Seedling card
        
        // Act
        SUT.trashSeedlingCards()
        
        // Assert
        assertEquals(2, SUT.cardCount)
        assertTrue(SUT.hasCard(CARD_ID_1)) // Bloom card should remain
        assertTrue(SUT.hasCard(CARD_ID_2)) // Root card should remain
        assertFalse(SUT.hasCard(seedlingCardId)) // Seedling card should be removed
    }
    
    @Test
    fun trashSeedlingCards_whenNoSeedlingCardsExist_doesNothing() {
        // Arrange
        SUT.addCard(CARD_ID_1) // Bloom card
        SUT.addCard(CARD_ID_2) // Root card
        
        // Act
        SUT.trashSeedlingCards()
        
        // Assert
        assertEquals(2, SUT.cardCount)
        assertTrue(SUT.hasCard(CARD_ID_1))
        assertTrue(SUT.hasCard(CARD_ID_2))
    }
} 
